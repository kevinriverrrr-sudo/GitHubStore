package com.githubstore.data.model

import com.google.gson.annotations.SerializedName

data class GithubRepo(
    val id: Long,
    val name: String,
    @SerializedName("full_name")
    val full_name: String,
    val owner: Owner? = null,
    val description: String? = null,
    @SerializedName("html_url")
    val html_url: String = "",
    val homepage: String? = null,
    val language: String? = null,
    @SerializedName("stargazers_count")
    val stargazers_count: Int = 0,
    @SerializedName("forks_count")
    val forks_count: Int = 0,
    @SerializedName("open_issues_count")
    val open_issues_count: Int = 0,
    @SerializedName("topics")
    val topics: List<String> = emptyList(),
    val license: LicenseInfo? = null,
    @SerializedName("updated_at")
    val updated_at: String = "",
    @SerializedName("created_at")
    val created_at: String = "",
    val size: Long = 0,
    @SerializedName("default_branch")
    val default_branch: String = "main"
)

data class Owner(
    val login: String = "",
    val id: Long = 0,
    @SerializedName("avatar_url")
    val avatar_url: String = "",
    @SerializedName("html_url")
    val html_url: String = ""
)

data class LicenseInfo(
    val key: String = "",
    val name: String = "",
    @SerializedName("spdx_id")
    val spdx_id: String? = null
)

data class GithubRelease(
    val id: Long,
    @SerializedName("tag_name")
    val tag_name: String = "",
    val name: String? = null,
    val body: String? = null,
    @SerializedName("published_at")
    val published_at: String = "",
    val assets: List<ReleaseAsset> = emptyList(),
    @SerializedName("html_url")
    val html_url: String = "",
    val prerelease: Boolean = false,
    val draft: Boolean = false
)

data class ReleaseAsset(
    val id: Long,
    val name: String = "",
    val size: Long = 0,
    @SerializedName("browser_download_url")
    val download_url: String = "",
    @SerializedName("content_type")
    val content_type: String = "",
    @SerializedName("download_count")
    val download_count: Int = 0
)

data class ReadmeContent(
    val content: String = "",
    val encoding: String = "",
    @SerializedName("html_url")
    val html_url: String = ""
)

data class SearchResponse(
    @SerializedName("total_count")
    val total_count: Int = 0,
    @SerializedName("incomplete_results")
    val incomplete_results: Boolean = false,
    val items: List<GithubRepo> = emptyList()
)

data class ApiRateLimit(
    val limit: Int = 0,
    val remaining: Int = 0,
    val reset: Long = 0,
    val used: Int = 0
)

data class DeviceCodeResponse(
    @SerializedName("device_code")
    val deviceCode: String = "",
    @SerializedName("user_code")
    val userCode: String = "",
    @SerializedName("verification_uri")
    val verificationUri: String = "",
    @SerializedName("expires_in")
    val expiresIn: Int = 0,
    val interval: Int = 5
)

data class AccessTokenResponse(
    @SerializedName("access_token")
    val accessToken: String? = null,
    @SerializedName("token_type")
    val tokenType: String? = null,
    val scope: String? = null,
    val error: String? = null,
    @SerializedName("error_description")
    val errorDescription: String? = null
)

data class GithubUser(
    val login: String = "",
    val id: Long = 0,
    @SerializedName("avatar_url")
    val avatar_url: String = "",
    val name: String? = null
)
