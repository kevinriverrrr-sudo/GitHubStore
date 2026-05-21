package com.githubstore.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
                viewModel.setDownloadDir(uri.toString())
            } catch (_: Exception) {
                Toast.makeText(context, "Cannot access folder", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(uiState.deviceLoginSuccess) {
        if (uiState.deviceLoginSuccess) {
            Toast.makeText(context, "Signed in!", Toast.LENGTH_SHORT).show()
            viewModel.consumeDeviceLoginSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // GitHub Account section
            SettingsSection(title = "GitHub Account") {
                if (uiState.userLogin.isNotBlank()) {
                    SettingsItem(
                        icon = Icons.Default.AccountCircle,
                        title = "Signed in as ${uiState.userLogin}",
                        subtitle = "Tap to sign out",
                        onClick = { viewModel.signOut() }
                    )
                } else {
                    SettingsItem(
                        icon = Icons.AutoMirrored.Filled.Login,
                        title = "Sign in with GitHub",
                        subtitle = "Use device code login (recommended)",
                        onClick = { viewModel.startDeviceLogin() }
                    )
                }
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
                    subtitle = if (uiState.isProxyEnabled) {
                        "${uiState.proxyType.uppercase()} ${uiState.proxyHost}:${uiState.proxyPort}"
                    } else "Disabled",
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
                        onClick = { viewModel.loadRateLimit() }
                    )
                } ?: SettingsItem(
                    icon = Icons.Default.DataUsage,
                    title = "Rate Limit",
                    subtitle = "Tap to refresh",
                    onClick = { viewModel.loadRateLimit() }
                )
            }

            // About
            SettingsSection(title = "About") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Version",
                    subtitle = "2.1.0",
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
            currentType = uiState.proxyType,
            onDismiss = { showProxyDialog = false },
            onSave = { host, port, type ->
                viewModel.setProxy(host, port, type)
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

    // Device Code Login Dialog
    if (uiState.deviceLoginInProgress || uiState.deviceCode != null) {
        DeviceCodeDialog(
            userCode = uiState.deviceCode?.userCode,
            verificationUri = uiState.deviceCode?.verificationUri ?: "https://github.com/login/device",
            error = uiState.deviceLoginError,
            onCancel = { viewModel.cancelDeviceLogin() }
        )
    } else if (uiState.deviceLoginError != null) {
        ErrorDialog(
            message = uiState.deviceLoginError!!,
            onDismiss = { viewModel.cancelDeviceLogin() }
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
    currentType: String,
    onDismiss: () -> Unit,
    onSave: (String, Int, String) -> Unit
) {
    var host by remember { mutableStateOf(currentHost) }
    var port by remember { mutableStateOf(if (currentPort > 0) currentPort.toString() else "") }
    var type by remember { mutableStateOf(currentType) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Proxy Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = type.uppercase(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("http", "socks").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.uppercase()) },
                                onClick = {
                                    type = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it.trim() },
                    label = { Text("Host") },
                    placeholder = { Text("e.g., 127.0.0.1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter { c -> c.isDigit() }.take(5) },
                    label = { Text("Port") },
                    placeholder = { Text("e.g., 8080") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Leave host empty to disable proxy",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val portNum = port.toIntOrNull() ?: 0
                onSave(host, portNum, type)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
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
    var visible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("GitHub Token") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it.trim() },
                    label = { Text("Personal Access Token") },
                    placeholder = { Text("ghp_xxxxx...") },
                    visualTransformation = if (visible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
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
            TextButton(onClick = { onSave(token) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DeviceCodeDialog(
    userCode: String?,
    verificationUri: String,
    error: String?,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Sign in to GitHub") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (userCode == null) {
                    CircularProgressIndicator()
                    Text("Requesting device code...")
                } else {
                    Text("Open this URL in your browser:", textAlign = TextAlign.Center)
                    Text(
                        verificationUri,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(verificationUri)))
                            } catch (_: Exception) {}
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Then enter this code:", textAlign = TextAlign.Center)
                    Text(
                        userCode,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("user_code", userCode))
                            Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                        }
                    )
                    Text(
                        "Tap to copy or open in browser",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        "Waiting for you to approve...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            if (userCode != null) {
                TextButton(onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(verificationUri)))
                    } catch (_: Exception) {}
                }) { Text("Open browser") }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    )
}

@Composable
private fun ErrorDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Error") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}
