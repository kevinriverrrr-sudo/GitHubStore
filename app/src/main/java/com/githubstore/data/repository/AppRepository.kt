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

    suspend fun getTrending(page: Int = 1): RepositoryResult {
        return try {
            val repos = api.getTrendingRepositories(page = page)
            RepositoryResult.Success(repos, repos.size)
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

    // Stable broad queries: page 2+ returns consistent results with page 1.
    suspend fun getAndroidApps(page: Int = 1): RepositoryResult =
        searchApps(queryForCategory("android"), page = page)

    suspend fun getDesktopApps(page: Int = 1): RepositoryResult =
        searchApps(queryForCategory("desktop"), page = page)

    suspend fun getLinuxApps(page: Int = 1): RepositoryResult =
        searchApps(queryForCategory("linux"), page = page)

    suspend fun getIosApps(page: Int = 1): RepositoryResult =
        searchApps(queryForCategory("ios"), page = page)

    suspend fun getAllApps(page: Int = 1): RepositoryResult =
        searchApps(queryForCategory("all"), page = page)

    fun queryForCategory(category: String): String = when (category) {
        "android" -> "topic:android stars:>500"
        "desktop" -> "topic:desktop stars:>200"
        "linux" -> "topic:linux stars:>500"
        "ios" -> "topic:ios language:swift stars:>200"
        "all" -> "stars:>1000"
        else -> "stars:>500"
    }

    suspend fun getRepoDetails(owner: String, repo: String): GithubRepo? {
        if (owner.isBlank() || repo.isBlank()) return null
        return try {
            api.getRepository(owner, repo)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getReleases(owner: String, repo: String): List<GithubRelease> {
        if (owner.isBlank() || repo.isBlank()) return emptyList()
        return try {
            api.getReleases(owner, repo)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getLatestRelease(owner: String, repo: String): GithubRelease? {
        if (owner.isBlank() || repo.isBlank()) return null
        return try {
            api.getLatestRelease(owner, repo)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getReadme(owner: String, repo: String): String? {
        if (owner.isBlank() || repo.isBlank()) return null
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

    fun findInstallableAssets(releases: List<GithubRelease>): List<ReleaseAsset> {
        val installableExtensions = listOf(
            ".apk", ".aab", ".exe", ".msi", ".dmg",
            ".deb", ".rpm", ".appimage", ".flatpak", ".snap",
            ".zip", ".tar.gz"
        )
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
