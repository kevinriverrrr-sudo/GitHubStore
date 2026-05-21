package com.githubstore.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
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
import kotlinx.coroutines.flow.update
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
    val downloadingAssetId: Long? = null,
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
            _uiState.update { it.copy(isLoading = true, error = null) }

            val repo = repository.getRepoDetails(owner, repoName)
            if (repo != null) {
                _uiState.update { it.copy(
                    repo = repo,
                    isFavorite = favoritesManager.isFavorite(repo.full_name)
                ) }
            } else {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = "Failed to load repository"
                ) }
                return@launch
            }

            // Load releases
            val releases = repository.getReleases(owner, repoName)
            val installable = repository.findInstallableAssets(releases)
            _uiState.update { it.copy(
                releases = releases,
                installableAssets = installable
            ) }

            // Load readme
            val readme = repository.getReadme(owner, repoName)
            _uiState.update { it.copy(
                readme = readme,
                isLoading = false
            ) }
        }
    }

    fun toggleFavorite() {
        val repo = _uiState.value.repo ?: return
        val isNowFavorite = favoritesManager.toggleFavorite(repo.full_name)
        _uiState.update { it.copy(isFavorite = isNowFavorite) }
    }

    fun downloadAndInstall(context: Context, asset: ReleaseAsset) {
        if (_uiState.value.isDownloading) return
        if (asset.download_url.isBlank()) {
            Toast.makeText(context, "Download URL is missing", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(
                isDownloading = true,
                downloadProgress = 0f,
                downloadStatus = "downloading",
                downloadingAssetId = asset.id
            ) }

            try {
                val fileName = asset.name.ifBlank { "download_${asset.id}.bin" }
                val downloadsDir = File(context.getExternalFilesDir(null), "downloads").also {
                    if (!it.exists()) it.mkdirs()
                }
                val destFile = File(downloadsDir, fileName)
                if (destFile.exists()) destFile.delete()

                val success = repository.downloadFile(
                    url = asset.download_url,
                    destinationPath = destFile.absolutePath
                ) { bytesRead, totalBytes ->
                    val progress = if (totalBytes > 0) bytesRead.toFloat() / totalBytes else 0f
                    _uiState.update { it.copy(downloadProgress = progress) }
                }

                if (success && destFile.exists() && destFile.length() > 0) {
                    _uiState.update { it.copy(
                        isDownloading = false,
                        downloadStatus = "complete",
                        downloadingAssetId = null
                    ) }
                    if (fileName.endsWith(".apk", ignoreCase = true)) {
                        installApk(context, destFile)
                    } else {
                        Toast.makeText(
                            context,
                            "Downloaded: ${destFile.absolutePath}",
                            Toast.LENGTH_LONG
                        ).show()
                        openFile(context, destFile)
                    }
                } else {
                    if (destFile.exists()) destFile.delete()
                    _uiState.update { it.copy(
                        isDownloading = false,
                        downloadStatus = "failed",
                        downloadingAssetId = null
                    ) }
                    Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(
                    isDownloading = false,
                    downloadStatus = "failed",
                    downloadingAssetId = null
                ) }
                Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun installApk(context: Context, file: File) {
        try {
            if (!file.exists()) {
                Toast.makeText(context, "APK file not found", Toast.LENGTH_SHORT).show()
                return
            }

            // On API 26+ confirm unknown sources permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val canInstall = context.packageManager.canRequestPackageInstalls()
                if (!canInstall) {
                    try {
                        val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                            .setData(Uri.parse("package:${context.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        Toast.makeText(
                            context,
                            "Allow installation from this app, then tap Install again",
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    } catch (_: Exception) {}
                }
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot install APK: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (_: Exception) {}
    }
}
