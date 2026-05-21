package com.githubstore.data.repository

import com.githubstore.data.api.GithubApi
import com.githubstore.data.model.*
import kotlinx.coroutines.CancellationException

class AppRepository(private val api: GithubApi) {

    suspend fun searchApps(
        query: String,
        page: Int = 1,
        perPage: Int = 30,
        sort: String = "stars"
    ): RepositoryResult {
        return try {
            val result = api.searchRepositories(query, sort = sort, page = page, perPage = perPage)
            if (result != null) {
                RepositoryResult.Success(result.items, result.total_count)
            } else {
                RepositoryResult.Error("Failed to load results")
            }
        } catch (e: GithubApi.RateLimitException) {
            RepositoryResult.RateLimited
        } catch (e: GithubApi.UnauthorizedException) {
            RepositoryResult.Error("Invalid token. Please re-login.")
        } catch (e: CancellationException) {
            throw e
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
            "ios" -> "swift"
            else -> null
        }
        return try {
            val repos = api.getTrendingRepositories(language = lang, page = page)
            if (repos.isNotEmpty()) {
                RepositoryResult.Success(repos, repos.size)
            } else {
                // Fallback to plain popular search if trending is empty (rare)
                searchApps("stars:>1000", page = page)
            }
        } catch (e: GithubApi.RateLimitException) {
            RepositoryResult.RateLimited
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            RepositoryResult.Error("Network error")
        }
    }

    // More targeted Android query: must have android topic AND stars
    suspend fun getAndroidApps(page: Int = 1): RepositoryResult {
        // Filter strictly by android-related topics to match real Android apps, not generic "android" mentions.
        return searchApps(
            "topic:android-application stars:>200",
            page = page
        ).let { result ->
            if (result is RepositoryResult.Success && result.repos.size < 5 && page == 1) {
                // Broaden if too few hits
                searchApps("topic:android stars:>500", page = page)
            } else result
        }
    }

    suspend fun getDesktopApps(page: Int = 1): RepositoryResult {
        return searchApps(
            "topic:desktop-application stars:>200",
            page = page
        ).let { result ->
            if (result is RepositoryResult.Success && result.repos.size < 5 && page == 1) {
                searchApps("topic:desktop-app stars:>200", page = page)
            } else result
        }
    }

    suspend fun getLinuxApps(page: Int = 1): RepositoryResult {
        return searchApps(
            "topic:linux-app stars:>100",
            page = page
        ).let { result ->
            if (result is RepositoryResult.Success && result.repos.size < 5 && page == 1) {
                searchApps("topic:linux stars:>500", page = page)
            } else result
        }
    }

    suspend fun getIosApps(page: Int = 1): RepositoryResult {
        return searchApps("topic:ios language:swift stars:>200", page = page)
    }

    suspend fun getAllApps(page: Int = 1): RepositoryResult {
        return searchApps("stars:>1000", page = page)
    }

    suspend fun getRepoDetails(owner: String, repo: String): GithubRepo? {
        return try {
            api.getRepository(owner, repo)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getReleases(owner: String, repo: String): List<GithubRelease> {
        return try {
            api.getReleases(owner, repo)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getLatestRelease(owner: String, repo: String): GithubRelease? {
        return try {
            api.getLatestRelease(owner, repo)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getReadme(owner: String, repo: String): String? {
        return try {
            api.getReadme(owner, repo)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getRateLimit(): ApiRateLimit? {
        return try {
            api.getRateLimit()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getCurrentUser(): GithubUser? {
        return try {
            api.getAuthenticatedUser()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun downloadFile(url: String, destinationPath: String, onProgress: (Long, Long) -> Unit): Boolean {
        return api.downloadFile(url, destinationPath, onProgress)
    }

    fun findApkAsset(releases: List<GithubRelease>): ReleaseAsset? {
        for (release in releases) {
            if (release.draft) continue
            for (asset in release.assets) {
                if (asset.name.endsWith(".apk", ignoreCase = true) && asset.download_url.isNotBlank()) {
                    return asset
                }
            }
        }
        return null
    }

    fun findInstallableAssets(releases: List<GithubRelease>): List<ReleaseAsset> {
        val installableExtensions = listOf(
            ".apk", ".aab", ".exe", ".msi", ".dmg",
            ".deb", ".rpm", ".appimage", ".flatpak", ".snap",
            ".zip", ".tar.gz"
        )
        // Skip drafts, return only assets with a valid download URL
        return releases.asSequence()
            .filter { !it.draft }
            .flatMap { release ->
                release.assets.asSequence().filter { asset ->
                    asset.download_url.isNotBlank() &&
                    installableExtensions.any { ext -> asset.name.endsWith(ext, ignoreCase = true) }
                }
            }
            .distinctBy { it.id }
            .toList()
    }
}

sealed class RepositoryResult {
    data class Success(val repos: List<GithubRepo>, val totalCount: Int) : RepositoryResult()
    data class Error(val message: String) : RepositoryResult()
    object RateLimited : RepositoryResult()
}
