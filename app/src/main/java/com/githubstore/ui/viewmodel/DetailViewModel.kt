package com.githubstore.ui.viewmodel

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.githubstore.data.model.GithubRelease
import com.githubstore.data.model.GithubRepo
import com.githubstore.data.model.ReleaseAsset
import com.githubstore.data.repository.AppRepository
import com.githubstore.util.FavoritesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetailUiState(
    val repo: GithubRepo? = null,
    val releases: List<GithubRelease> = emptyList(),
    val readme: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFavorite: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val downloadStatus: String? = null,
    val installableAssets: List<ReleaseAsset> = emptyList()
)

class DetailViewModel(
    private val repository: AppRepository,
    private val favoritesManager: FavoritesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun loadRepoDetails(owner: String, repoName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val repo = repository.getRepoDetails(owner, repoName)
            if (repo != null) {
                _uiState.value = _uiState.value.copy(
                    repo = repo,
                    isFavorite = favoritesManager.isFavorite(repo.full_name)
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load repository"
                )
                return@launch
            }

            // Load releases
            val releases = repository.getReleases(owner, repoName)
            val installable = repository.findInstallableAssets(releases)
            _uiState.value = _uiState.value.copy(
                releases = releases,
                installableAssets = installable
            )

            // Load readme
            val readme = repository.getReadme(owner, repoName)
            _uiState.value = _uiState.value.copy(
                readme = readme,
                isLoading = false
            )
        }
    }

    fun toggleFavorite() {
        val repo = _uiState.value.repo ?: return
        val isNowFavorite = favoritesManager.toggleFavorite(repo.full_name)
        _uiState.value = _uiState.value.copy(isFavorite = isNowFavorite)
    }

    fun downloadAndInstall(
        context: Context,
        asset: ReleaseAsset,
        downloadDir: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDownloading = true,
                downloadProgress = 0f,
                downloadStatus = "downloading"
            )

            val fileName = asset.name
            val destDir = downloadDir ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
            val destPath = "$destDir/$fileName"

            val success = repository.downloadFile(
                url = asset.download_url,
                destinationPath = destPath
            ) { bytesRead, totalBytes ->
                val progress = if (totalBytes > 0) bytesRead.toFloat() / totalBytes else 0f
                _uiState.value = _uiState.value.copy(downloadProgress = progress)
            }

            if (success) {
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    downloadStatus = "complete"
                )
                // Trigger install if APK
                if (fileName.endsWith(".apk", ignoreCase = true)) {
                    installApk(context, destPath)
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    downloadStatus = "failed"
                )
            }
        }
    }

    private fun installApk(context: Context, filePath: String) {
        try {
            val file = java.io.File(filePath)
            if (!file.exists()) return

            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
