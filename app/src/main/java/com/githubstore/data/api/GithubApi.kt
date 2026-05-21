package com.githubstore.data.api

import com.githubstore.data.model.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class GithubApi(
    @Volatile private var authToken: String? = null,
    @Volatile private var proxyHost: String? = null,
    @Volatile private var proxyPort: Int = 0,
    @Volatile private var proxyType: String = "http"
) {
    private val gson: Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        .create()

    private val baseUrl = "https://api.github.com"

    // Public client ID for GitHub OAuth Device Flow. Defaults to GitHub CLI's
    // well-known public client ID so the flow works out-of-the-box. Users can
    // override with their own OAuth App via setOAuthClientId().
    @Volatile
    private var oauthClientId: String = "178c6fc778ccc68e1d6a"

    @Volatile
    private var _client: OkHttpClient? = null
    val client: OkHttpClient get() = _client ?: synchronized(this) {
        _client ?: buildHttpClient().also { _client = it }
    }

    private fun buildHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            // Common headers for all requests
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                    .header("User-Agent", "GitHubStore-Android/2.1.1")
                if (original.header("Accept") == null) {
                    builder.header("Accept", "application/vnd.github.v3+json")
                }
                chain.proceed(builder.build())
            }
            // Authorization is added only for api.github.com requests, and is
            // never sent to other hosts (e.g., release asset CDN, OAuth login).
            .addNetworkInterceptor { chain ->
                val original = chain.request()
                val host = original.url.host
                val isApiHost = host.equals("api.github.com", ignoreCase = true)
                val builder = original.newBuilder()
                if (isApiHost) {
                    builder.header("X-GitHub-Api-Version", "2022-11-28")
                    authToken?.takeIf { it.isNotBlank() }?.let {
                        builder.header("Authorization", "Bearer $it")
                    }
                } else {
                    // Defensive: strip any auth header that may have leaked in via redirects.
                    builder.removeHeader("Authorization")
                }
                chain.proceed(builder.build())
            }

        val host = proxyHost
        if (!host.isNullOrBlank() && proxyPort in 1..65535) {
            try {
                val type = if (proxyType.equals("socks", ignoreCase = true)) {
                    Proxy.Type.SOCKS
                } else {
                    Proxy.Type.HTTP
                }
                val proxy = Proxy(type, InetSocketAddress.createUnresolved(host, proxyPort))
                builder.proxy(proxy)
            } catch (_: Exception) { /* invalid proxy — fall back to direct */ }
        }

        return builder.build()
    }

    fun updateProxy(host: String?, port: Int, type: String = "http") {
        proxyHost = host
        proxyPort = port
        proxyType = type
        synchronized(this) {
            _client = buildHttpClient()
        }
    }

    fun updateToken(token: String?) {
        authToken = token?.takeIf { it.isNotBlank() }
    }

    fun getToken(): String? = authToken

    fun setOAuthClientId(clientId: String) {
        if (clientId.isNotBlank()) oauthClientId = clientId
    }

    fun getOAuthClientId(): String = oauthClientId

    private suspend fun <T> executeRequest(request: Request, typeToken: java.lang.reflect.Type): T? {
        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        if (response.code == 403 || response.code == 429) {
                            val remaining = response.header("X-RateLimit-Remaining")
                            if (remaining == "0" || response.code == 429) {
                                throw RateLimitException("API rate limit exceeded")
                            }
                        }
                        if (response.code == 401) {
                            throw UnauthorizedException("Invalid GitHub token")
                        }
                        return@withContext null
                    }
                    val body = response.body?.string() ?: return@withContext null
                    gson.fromJson<T>(body, typeToken)
                }
            } catch (e: RateLimitException) {
                throw e
            } catch (e: UnauthorizedException) {
                throw e
            } catch (e: CancellationException) {
                throw e
            } catch (_: IOException) {
                null
            } catch (_: Exception) {
                null
            }
        }
    }

    private suspend fun executeStringRequest(request: Request): String? {
        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    response.body?.string()
                }
            } catch (e: CancellationException) {
                throw e
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
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
        val url = "$baseUrl/search/repositories?q=$encodedQuery" +
                "&sort=$sort&order=$order&page=$page&per_page=$perPage"
        val request = Request.Builder().url(url).build()
        val type = object : TypeToken<SearchResponse>() {}.type
        return executeRequest(request, type)
    }

    suspend fun getTrendingRepositories(
        since: String = "weekly",
        page: Int = 1,
        perPage: Int = 30
    ): List<GithubRepo> {
        val dateFilter = when (since) {
            "daily" -> "pushed:>${java.time.LocalDate.now().minusDays(2)}"
            "monthly" -> "pushed:>${java.time.LocalDate.now().minusMonths(1)}"
            else -> "pushed:>${java.time.LocalDate.now().minusWeeks(1)}"
        }
        val query = "$dateFilter stars:>100"
        val result = searchRepositories(query, page = page, perPage = perPage)
        return result?.items ?: emptyList()
    }

    suspend fun getRepository(owner: String, repo: String): GithubRepo? {
        val url = "$baseUrl/repos/${URLEncoder.encode(owner, "UTF-8")}/${URLEncoder.encode(repo, "UTF-8")}"
        val request = Request.Builder().url(url).build()
        val type = object : TypeToken<GithubRepo>() {}.type
        return executeRequest(request, type)
    }

    suspend fun getReleases(owner: String, repo: String): List<GithubRelease> {
        val url = "$baseUrl/repos/${URLEncoder.encode(owner, "UTF-8")}/${URLEncoder.encode(repo, "UTF-8")}/releases?per_page=20"
        val request = Request.Builder().url(url).build()
        val type = object : TypeToken<List<GithubRelease>>() {}.type
        return executeRequest(request, type) ?: emptyList()
    }

    suspend fun getLatestRelease(owner: String, repo: String): GithubRelease? {
        val url = "$baseUrl/repos/${URLEncoder.encode(owner, "UTF-8")}/${URLEncoder.encode(repo, "UTF-8")}/releases/latest"
        val request = Request.Builder().url(url).build()
        val type = object : TypeToken<GithubRelease>() {}.type
        return executeRequest(request, type)
    }

    suspend fun getReadme(owner: String, repo: String): String? {
        val url = "$baseUrl/repos/${URLEncoder.encode(owner, "UTF-8")}/${URLEncoder.encode(repo, "UTF-8")}/readme"
        val request = Request.Builder().url(url).build()
        val type = object : TypeToken<ReadmeContent>() {}.type
        val readme = executeRequest<ReadmeContent>(request, type) ?: return null
        return if (readme.encoding.equals("base64", ignoreCase = true)) {
            try {
                android.util.Base64.decode(readme.content, android.util.Base64.DEFAULT)
                    .toString(Charsets.UTF_8)
            } catch (_: Exception) {
                readme.content
            }
        } else {
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
                limit = core.getAsJsonPrimitive("limit")?.asInt ?: 0,
                remaining = core.getAsJsonPrimitive("remaining")?.asInt ?: 0,
                reset = core.getAsJsonPrimitive("reset")?.asLong ?: 0,
                used = core.getAsJsonPrimitive("used")?.asInt ?: 0
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getAuthenticatedUser(): GithubUser? {
        if (authToken.isNullOrBlank()) return null
        val url = "$baseUrl/user"
        val request = Request.Builder().url(url).build()
        val type = object : TypeToken<GithubUser>() {}.type
        return executeRequest(request, type)
    }

    // === OAuth Device Flow (github.com, NOT api.github.com — no auth header sent) ===

    suspend fun requestDeviceCode(scopes: String = "repo,read:user"): DeviceCodeResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val body = FormBody.Builder()
                    .add("client_id", oauthClientId)
                    .add("scope", scopes)
                    .build()
                val request = Request.Builder()
                    .url("https://github.com/login/device/code")
                    .post(body)
                    .header("Accept", "application/json")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val text = response.body?.string() ?: return@withContext null
                    gson.fromJson(text, DeviceCodeResponse::class.java)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun pollAccessToken(deviceCode: String): AccessTokenResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val body = FormBody.Builder()
                    .add("client_id", oauthClientId)
                    .add("device_code", deviceCode)
                    .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                    .build()
                val request = Request.Builder()
                    .url("https://github.com/login/oauth/access_token")
                    .post(body)
                    .header("Accept", "application/json")
                    .build()
                client.newCall(request).execute().use { response ->
                    val text = response.body?.string() ?: return@withContext null
                    gson.fromJson(text, AccessTokenResponse::class.java)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun downloadFile(url: String, destinationPath: String, onProgress: (Long, Long) -> Unit): Boolean {
        if (url.isBlank()) return false
        return withContext(Dispatchers.IO) {
            try {
                val requestBuilder = Request.Builder().url(url)
                if (url.startsWith("https://api.github.com/")) {
                    requestBuilder.header("Accept", "application/octet-stream")
                }
                val request = requestBuilder.build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext false
                    val body = response.body ?: return@withContext false
                    val contentLength = body.contentLength()
                    var bytesRead = 0L

                    val destFile = java.io.File(destinationPath)
                    destFile.parentFile?.mkdirs()

                    destFile.outputStream().use { output ->
                        body.byteStream().use { input ->
                            val buffer = ByteArray(8192)
                            var bytes = input.read(buffer)
                            var lastUpdateTime = 0L
                            while (bytes >= 0) {
                                output.write(buffer, 0, bytes)
                                bytesRead += bytes
                                val now = System.currentTimeMillis()
                                if (now - lastUpdateTime > 100) {
                                    withContext(Dispatchers.Main) {
                                        onProgress(bytesRead, contentLength)
                                    }
                                    lastUpdateTime = now
                                }
                                bytes = input.read(buffer)
                            }
                        }
                    }
                    withContext(Dispatchers.Main) {
                        onProgress(bytesRead, contentLength)
                    }
                    true
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                false
            }
        }
    }

    class RateLimitException(message: String) : Exception(message)
    class UnauthorizedException(message: String) : Exception(message)
}
