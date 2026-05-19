package com.githubstore.data.model

import com.google.gson.annotations.SerializedName

data class GithubRepo(
    val id: Long,
    val name: String,
    val full_name: String,
    val owner: Owner? = null,
    val description: String? = null,
    val html_url: String = "",
    val homepage: String? = null,
    val language: String? = null,
    val stargazers_count: Int = 0,
    val forks_count: Int = 0,
    val open_issues_count: Int = 0,
    @SerializedName("topics")
    val topics: List<String> = emptyList(),
    val license: LicenseInfo? = null,
    val updated_at: String = "",
    val created_at: String = "",
    val size: Long = 0,
    val default_branch: String = "main"
)

data class Owner(
    val login: String = "",
    val id: Long = 0,
    val avatar_url: String = "",
    val html_url: String = ""
)

data class LicenseInfo(
    val key: String = "",
    val name: String = "",
    val spdx_id: String? = null
)

data class GithubRelease(
    val id: Long,
    val tag_name: String = "",
    val name: String? = null,
    val body: String? = null,
    val published_at: String = "",
    val assets: List<ReleaseAsset> = emptyList(),
    val html_url: String = ""
)

data class ReleaseAsset(
    val id: Long,
    val name: String = "",
    val size: Long = 0,
    val download_url: String = "",
    val content_type: String = "",
    val download_count: Int = 0
)

data class ReadmeContent(
    val content: String = "",
    val encoding: String = "",
    val html_url: String = ""
)

data class SearchResponse(
    val total_count: Int = 0,
    val incomplete_results: Boolean = false,
    val items: List<GithubRepo> = emptyList()
)

data class ApiRateLimit(
    val limit: Int = 0,
    val remaining: Int = 0,
    val reset: Long = 0,
    val used: Int = 0
)
