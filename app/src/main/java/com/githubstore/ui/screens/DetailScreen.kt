package com.githubstore.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.githubstore.data.model.ReleaseAsset
import com.githubstore.ui.theme.*
import com.githubstore.ui.viewmodel.DetailViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    owner: String,
    repoName: String,
    downloadDir: String,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        repoName,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Favorite
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (uiState.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (uiState.isFavorite) FavoriteRed else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Share
                    uiState.repo?.let { repo ->
                        IconButton(onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, repo.html_url)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                        // Open in browser
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(repo.html_url))
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.OpenInNew, contentDescription = "Open in browser")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.error ?: "Error")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadRepoDetails(owner, repoName) }) {
                            Text("Retry")
                        }
                    }
                }
            }
            uiState.repo != null -> {
                val repo = uiState.repo!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header with avatar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = repo.owner.avatar_url,
                            contentDescription = repo.owner.login,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = repo.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = repo.owner.login,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Stats
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(Icons.Default.Star, "${formatCount(repo.stargazers_count)}", "Stars", StarYellow)
                        StatItem(Icons.Default.CallSplit, "${formatCount(repo.forks_count)}", "Forks", MaterialTheme.colorScheme.primary)
                        StatItem(Icons.Default.Report, "${formatCount(repo.open_issues_count)}", "Issues", MaterialTheme.colorScheme.tertiary)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Description
                    if (!repo.description.isNullOrBlank()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Description",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    repo.description!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Info card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            InfoRow("Language", repo.language ?: "Unknown")
                            repo.license?.let { InfoRow("License", it.name) }
                            InfoRow("Size", "${formatSize(repo.size)}")
                            InfoRow("Updated", formatDate(repo.updated_at))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Installable assets
                    if (uiState.installableAssets.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Available Downloads",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                uiState.installableAssets.forEach { asset ->
                                    DownloadRow(
                                        asset = asset,
                                        isDownloading = uiState.isDownloading,
                                        downloadProgress = uiState.downloadProgress,
                                        onDownload = {
                                            viewModel.downloadAndInstall(context, asset, downloadDir)
                                        }
                                    )
                                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "No installable files found in releases",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                uiState.repo?.let { repo ->
                                    TextButton(onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(repo.html_url))
                                        context.startActivity(intent)
                                    }) {
                                        Text("View on GitHub")
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Readme
                    if (uiState.readme != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "README",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                val maxReadmeLength = 2000
                                val readmeText = if (uiState.readme!!.length > maxReadmeLength) {
                                    uiState.readme!!.substring(0, maxReadmeLength) + "..."
                                } else {
                                    uiState.readme!!
                                }
                                Text(
                                    readmeText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DownloadRow(
    asset: ReleaseAsset,
    isDownloading: Boolean,
    downloadProgress: Float,
    onDownload: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                asset.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                formatSize(asset.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isDownloading) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = downloadProgress,
                    modifier = Modifier.fillMaxWidth(0.6f),
                    color = InstallBlue
                )
            }
        }
        Button(
            onClick = onDownload,
            enabled = !isDownloading,
            colors = ButtonDefaults.buttonColors(containerColor = InstallBlue)
        ) {
            if (isDownloading) {
                Text("Downloading")
            } else {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Install")
            }
        }
    }
}

private fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
    count >= 1_000 -> String.format("%.1fk", count / 1_000.0)
    else -> count.toString()
}

private fun formatSize(kb: Long): String = when {
    kb >= 1_024 -> String.format("%.1f MB", kb / 1024.0)
    else -> "$kb KB"
}

private fun formatDate(dateStr: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        val date = inputFormat.parse(dateStr)
        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        date?.let { outputFormat.format(it) } ?: dateStr
    } catch (_: Exception) {
        dateStr
    }
}
