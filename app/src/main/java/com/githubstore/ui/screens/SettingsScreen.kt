package com.githubstore.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.githubstore.ui.viewmodel.SettingsViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showProxyDialog by remember { mutableStateOf(false) }
    var showTokenDialog by remember { mutableStateOf(false) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
            viewModel.setDownloadDir(uri.toString())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // GitHub Token section
            SettingsSection(title = "GitHub Account") {
                SettingsItem(
                    icon = Icons.Default.Key,
                    title = "GitHub Token",
                    subtitle = if (uiState.authToken.isNotBlank()) "Token configured" else "Not configured (60 req/h)",
                    onClick = { showTokenDialog = true }
                )
            }

            // Theme
            SettingsSection(title = "Appearance") {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "Theme",
                    subtitle = when (uiState.theme) {
                        "dark" -> "Dark"
                        "light" -> "Light"
                        else -> "System"
                    },
                    onClick = {
                        val newTheme = when (uiState.theme) {
                            "dark" -> "light"
                            "light" -> "system"
                            else -> "dark"
                        }
                        viewModel.setTheme(newTheme)
                    }
                )

                SettingsItem(
                    icon = Icons.Default.Language,
                    title = "Language",
                    subtitle = when (uiState.language) {
                        "ru" -> "Русский"
                        else -> "English"
                    },
                    onClick = {
                        val newLang = if (uiState.language == "ru") "en" else "ru"
                        viewModel.setLanguage(newLang)
                    }
                )
            }

            // Network
            SettingsSection(title = "Network") {
                SettingsItem(
                    icon = Icons.Default.VpnLock,
                    title = "Proxy",
                    subtitle = if (uiState.isProxyEnabled) "${uiState.proxyHost}:${uiState.proxyPort}" else "Disabled",
                    onClick = { showProxyDialog = true }
                )
            }

            // Downloads
            SettingsSection(title = "Downloads") {
                SettingsItem(
                    icon = Icons.Default.Folder,
                    title = "Download Folder",
                    subtitle = if (uiState.downloadDir.isNotBlank()) "Custom folder set" else "Default (Downloads)",
                    onClick = { folderPickerLauncher.launch(null) }
                )
            }

            // API Status
            SettingsSection(title = "API Status") {
                uiState.rateLimit?.let { limit ->
                    SettingsItem(
                        icon = Icons.Default.DataUsage,
                        title = "Rate Limit",
                        subtitle = "${limit.remaining} / ${limit.limit} remaining",
                        onClick = {}
                    )
                }
            }

            // About
            SettingsSection(title = "About") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Version",
                    subtitle = "1.0.0",
                    onClick = {}
                )
                SettingsItem(
                    icon = Icons.Default.Code,
                    title = "GitHub Store",
                    subtitle = "Discover and install open source apps from GitHub",
                    onClick = {}
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Proxy Dialog
    if (showProxyDialog) {
        ProxyDialog(
            currentHost = uiState.proxyHost,
            currentPort = uiState.proxyPort,
            onDismiss = { showProxyDialog = false },
            onSave = { host, port ->
                viewModel.setProxy(host, port)
                showProxyDialog = false
            }
        )
    }

    // Token Dialog
    if (showTokenDialog) {
        TokenDialog(
            currentToken = uiState.authToken,
            onDismiss = { showTokenDialog = false },
            onSave = { token ->
                viewModel.setAuthToken(token)
                showTokenDialog = false
            }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProxyDialog(
    currentHost: String,
    currentPort: Int,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit
) {
    var host by remember { mutableStateOf(currentHost) }
    var port by remember { mutableStateOf(if (currentPort > 0) currentPort.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Proxy Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Host") },
                    placeholder = { Text("e.g., 127.0.0.1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter { c -> c.isDigit() } },
                    label = { Text("Port") },
                    placeholder = { Text("e.g., 8080") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val portNum = port.toIntOrNull() ?: 0
                onSave(host, portNum)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TokenDialog(
    currentToken: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var token by remember { mutableStateOf(currentToken) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("GitHub Token") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Personal Access Token") },
                    placeholder = { Text("ghp_xxxxx...") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Optional. Increases API limit to 5000 requests/hour.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(token) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
