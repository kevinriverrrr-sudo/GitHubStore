package com.githubstore.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.githubstore.data.model.GithubRelease
import com.githubstore.data.model.ReleaseAsset
import com.githubstore.data.repository.AppRepository
import com.githubstore.util.FavoritesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class DetailUiState(
    val repo: com.githubstore.data.model.GithubRepo? = null,
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
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

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

            try {
                val fileName = asset.name

                // Use app's internal cache directory to avoid scoped storage issues
                val cacheDir = File(context.cacheDir, "downloads")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                val destPath = File(cacheDir, fileName).absolutePath

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
                    if (fileName.endsWith(".apk", ignoreCase = true)) {
                        installApk(context, destPath)
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isDownloading = false,
                        downloadStatus = "failed"
                    )
                }
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    downloadStatus = "failed"
                )
            }
        }
    }

    private fun installApk(context: Context, filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) return

            val uri: Uri = try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } catch (_: Exception) {
                @Suppress("DEPRECATION")
                Uri.fromFile(file)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
