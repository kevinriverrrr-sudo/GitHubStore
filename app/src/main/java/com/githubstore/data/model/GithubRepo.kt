package com.githubstore.data.model

import com.google.gson.annotations.SerializedName

data class GithubRepo(
    val id: Long,
    val name: String,
    val full_name: String,
    val owner: Owner,
    val description: String?,
    val html_url: String,
    val homepage: String?,
    val language: String?,
    val stargazers_count: Int,
    val forks_count: Int,
    val open_issues_count: Int,
    val topics: List<String> = emptyList(),
    val license: LicenseInfo?,
    val updated_at: String,
    val created_at: String,
    val size: Long,
    val default_branch: String = "main",
    @SerializedName("topics")
    val repoTopics: List<String>? = null
)

data class Owner(
    val login: String,
    val id: Long,
    val avatar_url: String,
    val html_url: String
)

data class LicenseInfo(
    val key: String,
    val name: String,
    val spdx_id: String?
)

data class GithubRelease(
    val id: Long,
    val tag_name: String,
    val name: String?,
    val body: String?,
    val published_at: String,
    val assets: List<ReleaseAsset>,
    val html_url: String
)

data class ReleaseAsset(
    val id: Long,
    val name: String,
    val size: Long,
    val download_url: String,
    val content_type: String,
    val download_count: Int
)

data class ReadmeContent(
    val content: String,
    val encoding: String,
    val html_url: String
)

data class SearchResponse(
    val total_count: Int,
    val incomplete_results: Boolean,
    val items: List<GithubRepo>
)

data class ApiRateLimit(
    val limit: Int,
    val remaining: Int,
    val reset: Long,
    val used: Int
)
