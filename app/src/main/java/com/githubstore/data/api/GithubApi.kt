package com.githubstore.data.api

import com.githubstore.data.model.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

class GithubApi(
    private var authToken: String? = null,
    private var proxyHost: String? = null,
    private var proxyPort: Int = 0
) {
    private val gson: Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        .create()

    private val baseUrl = "https://api.github.com"

    private var _client: OkHttpClient? = null
    val client: OkHttpClient get() = _client ?: buildHttpClient().also { _client = it }

    private fun buildHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Accept", "application/vnd.github.v3+json")
                    .addHeader("User-Agent", "GitHubStore-App")
                authToken?.let {
                    request.addHeader("Authorization", "Bearer $it")
                }
                chain.proceed(request.build())
            }

        if (!proxyHost.isNullOrBlank() && proxyPort > 0) {
            try {
                val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort))
                builder.proxy(proxy)
            } catch (_: Exception) {}
        }

        return builder.build()
    }

    fun updateProxy(host: String?, port: Int) {
        proxyHost = host
        proxyPort = port
        _client = buildHttpClient()
    }

    fun updateToken(token: String?) {
        authToken = token
    }

    private suspend fun <T> executeRequest(request: Request, typeToken: java.lang.reflect.Type): T? {
        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    if (response.code == 403 || response.code == 429) {
                        throw RateLimitException("API rate limit exceeded")
                    }
                    return@withContext null
                }
                val body = response.body?.string() ?: return@withContext null
                gson.fromJson<T>(body, typeToken)
            } catch (e: RateLimitException) {
                throw e
            } catch (e: IOException) {
                null
            }
        }
    }

    private suspend fun executeStringRequest(request: Request): String? {
        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext null
                response.body?.string()
            } catch (_: IOException) {
                null
            }
        }
    }

    suspend fun searchRepositories(
        query: String,
        sort: String = "stars",
        order: String = "desc",
        page: Int = 1,
        perPage: Int = 30
    ): SearchResponse? {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "${baseUrl}/search/repositories?q=$encodedQuery" +
                "&sort=$sort&order=$order&page=$page&per_page=$perPage"
        val request = Request.Builder().url(url).build()
        val type = object : TypeToken<SearchResponse>() {}.type
        return executeRequest(request, type)
    }

    suspend fun getTrendingRepositories(
        language: String? = null,
        since: String = "daily",
        page: Int = 1,
        perPage: Int = 30
    ): List<GithubRepo> {
        val dateFilter = when (since) {
            "weekly" -> "created:>${java.time.LocalDate.now().minusWeeks(1)}"
            "monthly" -> "created:>${java.time.LocalDate.now().minusMonths(1)}"
            else -> "created:>${java.time.LocalDate.now().minusDays(1)}"
        }
        val langFilter = if (!language.isNullOrBlank()) "language:$language " else ""
        val query = "$langFilter$dateFilter stars:>10"
        val result = searchRepositories(query, page = page, perPage = perPage)
        return result?.items ?: emptyList()
    }

    suspend fun getRepository(owner: String, repo: String): GithubRepo? {
        val url = "${baseUrl}/repos/$owner/$repo"
        val request = Request.Builder().url(url).build()
        val type = object : TypeToken<GithubRepo>() {}.type
        return executeRequest(request, type)
    }

    suspend fun getReleases(owner: String, repo: String): List<GithubRelease> {
        val url = "${baseUrl}/repos/$owner/$repo/releases"
        val request = Request.Builder().url(url).build()
        val type = object : TypeToken<List<GithubRelease>>() {}.type
        return executeRequest(request, type) ?: emptyList()
    }

    suspend fun getLatestRelease(owner: String, repo: String): GithubRelease? {
        val url = "${baseUrl}/repos/$owner/$repo/releases/latest"
        val request = Request.Builder().url(url).build()
        val type = object : TypeToken<GithubRelease>() {}.type
        return executeRequest(request, type)
    }

    suspend fun getReadme(owner: String, repo: String): String? {
        val url = "${baseUrl}/repos/$owner/$repo/readme"
        val request = Request.Builder().url(url).build()
        val type = object : TypeToken<ReadmeContent>() {}.type
        val readme = executeRequest<ReadmeContent>(request, type) ?: return null
        return try {
            android.util.Base64.decode(readme.content, android.util.Base64.DEFAULT)
                .toString(Charsets.UTF_8)
        } catch (_: Exception) {
            readme.content
        }
    }

    suspend fun getRateLimit(): ApiRateLimit? {
        val url = "$baseUrl/rate_limit"
        val request = Request.Builder().url(url).build()
        val responseJson = executeStringRequest(request) ?: return null
        return try {
            val jsonObject = JsonParser.parseString(responseJson).asJsonObject
            val resources = jsonObject.getAsJsonObject("resources")
            val core = resources.getAsJsonObject("core")
            ApiRateLimit(
                limit = core.get("limit").asInt,
                remaining = core.get("remaining").asInt,
                reset = core.get("reset").asLong,
                used = core.get("used").asInt
            )
        } catch (_: Exception) {
            null
        }
    }

    fun downloadFile(url: String, destinationPath: String, onProgress: (Long, Long) -> Unit): Boolean {
        return try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return false

            val body = response.body ?: return false
            val contentLength = body.contentLength()
            var bytesRead = 0L

            val destFile = java.io.File(destinationPath)
            destFile.parentFile?.mkdirs()

            destFile.outputStream().use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytes = input.read(buffer)
                    while (bytes >= 0) {
                        output.write(buffer, 0, bytes)
                        bytesRead += bytes
                        onProgress(bytesRead, contentLength)
                        bytes = input.read(buffer)
                    }
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    class RateLimitException(message: String) : Exception(message)
}
