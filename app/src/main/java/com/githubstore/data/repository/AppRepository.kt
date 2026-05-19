package com.githubstore.data.repository

import com.githubstore.data.api.GithubApi
import com.githubstore.data.model.*

class AppRepository(private val api: GithubApi) {

    suspend fun searchApps(
        query: String,
        page: Int = 1,
        perPage: Int = 30
    ): RepositoryResult {
        return try {
            val result = api.searchRepositories(query, page = page, perPage = perPage)
            if (result != null) {
                RepositoryResult.Success(result.items, result.total_count)
            } else {
                RepositoryResult.Error("Failed to load results")
            }
        } catch (e: GithubApi.RateLimitException) {
            RepositoryResult.RateLimited
        } catch (_: Exception) {
            RepositoryResult.Error("Network error")
        }
    }

    suspend fun getTrending(
        category: String? = null,
        page: Int = 1
    ): RepositoryResult {
        val lang = when (category) {
            "android" -> "kotlin"
            "desktop" -> null
            "linux" -> null
            "ios" -> "swift"
            else -> null
        }
        return try {
            val repos = api.getTrendingRepositories(language = lang, page = page)
            RepositoryResult.Success(repos, repos.size)
        } catch (e: GithubApi.RateLimitException) {
            RepositoryResult.RateLimited
        } catch (_: Exception) {
            RepositoryResult.Error("Network error")
        }
    }

    suspend fun getAndroidApps(page: Int = 1): RepositoryResult {
        return searchApps("android app topic:android language:kotlin stars:>50", page = page)
    }

    suspend fun getDesktopApps(page: Int = 1): RepositoryResult {
        return searchApps("desktop application topic:desktop stars:>50", page = page)
    }

    suspend fun getLinuxApps(page: Int = 1): RepositoryResult {
        return searchApps("linux application topic:linux stars:>50", page = page)
    }

    suspend fun getIosApps(page: Int = 1): RepositoryResult {
        return searchApps("ios app topic:ios language:swift stars:>50", page = page)
    }

    suspend fun getAllApps(page: Int = 1): RepositoryResult {
        return searchApps("stars:>100", page = page)
    }

    suspend fun getRepoDetails(owner: String, repo: String): GithubRepo? {
        return try {
            api.getRepository(owner, repo)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getReleases(owner: String, repo: String): List<GithubRelease> {
        return try {
            api.getReleases(owner, repo)
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getLatestRelease(owner: String, repo: String): GithubRelease? {
        return try {
            api.getLatestRelease(owner, repo)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getReadme(owner: String, repo: String): String? {
        return try {
            api.getReadme(owner, repo)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getRateLimit(): ApiRateLimit? {
        return api.getRateLimit()
    }

    fun downloadFile(url: String, destinationPath: String, onProgress: (Long, Long) -> Unit): Boolean {
        return api.downloadFile(url, destinationPath, onProgress)
    }

    fun findApkAsset(releases: List<GithubRelease>): ReleaseAsset? {
        for (release in releases) {
            for (asset in release.assets) {
                if (asset.name.endsWith(".apk", ignoreCase = true)) {
                    return asset
                }
            }
        }
        return null
    }

    fun findInstallableAssets(releases: List<GithubRelease>): List<ReleaseAsset> {
        val installableExtensions = listOf(".apk", ".exe", ".msi", ".dmg", ".deb", ".rpm", ".appimage", ".flatpak", ".snap")
        return releases.flatMap { release ->
            release.assets.filter { asset ->
                installableExtensions.any { ext -> asset.name.endsWith(ext, ignoreCase = true) }
            }
        }
    }
}

sealed class RepositoryResult {
    data class Success(val repos: List<GithubRepo>, val totalCount: Int) : RepositoryResult()
    data class Error(val message: String) : RepositoryResult()
    object RateLimited : RepositoryResult()
}
