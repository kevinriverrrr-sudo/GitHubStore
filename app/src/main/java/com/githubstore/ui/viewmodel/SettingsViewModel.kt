package com.githubstore.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.githubstore.data.api.GithubApi
import com.githubstore.data.model.ApiRateLimit
import com.githubstore.data.model.DeviceCodeResponse
import com.githubstore.data.repository.AppRepository
import com.githubstore.util.SettingsManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val theme: String = "system",
    val language: String = "en",
    val proxyHost: String = "",
    val proxyPort: Int = 0,
    val proxyType: String = "http",
    val downloadDir: String = "",
    val authToken: String = "",
    val userLogin: String = "",
    val rateLimit: ApiRateLimit? = null,
    val isProxyEnabled: Boolean = false,
    val deviceCode: DeviceCodeResponse? = null,
    val deviceLoginInProgress: Boolean = false,
    val deviceLoginError: String? = null,
    val deviceLoginSuccess: Boolean = false
)

class SettingsViewModel(
    private val settingsManager: SettingsManager,
    private val repository: AppRepository,
    private val api: GithubApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var deviceLoginJob: Job? = null

    init {
        viewModelScope.launch {
            settingsManager.themeFlow.collect { theme ->
                _uiState.update { it.copy(theme = theme) }
            }
        }
        viewModelScope.launch {
            settingsManager.languageFlow.collect { lang ->
                _uiState.update { it.copy(language = lang) }
            }
        }
        viewModelScope.launch {
            settingsManager.proxyHostFlow.collect { host ->
                _uiState.update { it.copy(proxyHost = host, isProxyEnabled = host.isNotBlank()) }
            }
        }
        viewModelScope.launch {
            settingsManager.proxyPortFlow.collect { port ->
                _uiState.update { it.copy(proxyPort = port) }
            }
        }
        viewModelScope.launch {
            settingsManager.proxyTypeFlow.collect { type ->
                _uiState.update { it.copy(proxyType = type) }
            }
        }
        viewModelScope.launch {
            settingsManager.downloadDirFlow.collect { dir ->
                _uiState.update { it.copy(downloadDir = dir) }
            }
        }
        viewModelScope.launch {
            settingsManager.authTokenFlow.collect { token ->
                _uiState.update { it.copy(authToken = token) }
            }
        }
        viewModelScope.launch {
            settingsManager.userLoginFlow.collect { login ->
                _uiState.update { it.copy(userLogin = login) }
            }
        }
        loadRateLimit()
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { settingsManager.setTheme(theme) }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch { settingsManager.setLanguage(language) }
    }

    fun setProxy(host: String, port: Int, type: String = "http") {
        viewModelScope.launch {
            if (host.isBlank()) {
                settingsManager.clearProxy()
                api.updateProxy(null, 0, "http")
            } else {
                settingsManager.setProxy(host, port, type)
                api.updateProxy(host, port, type)
            }
            // Refresh rate limit to verify proxy works
            loadRateLimit()
        }
    }

    fun setDownloadDir(dir: String) {
        viewModelScope.launch { settingsManager.setDownloadDir(dir) }
    }

    fun setAuthToken(token: String) {
        viewModelScope.launch {
            settingsManager.setAuthToken(token)
            api.updateToken(token.takeIf { it.isNotBlank() })
            if (token.isNotBlank()) {
                val user = repository.getCurrentUser()
                if (user != null) settingsManager.setUserLogin(user.login)
            } else {
                settingsManager.setUserLogin("")
            }
            loadRateLimit()
        }
    }

    fun loadRateLimit() {
        viewModelScope.launch {
            val limit = repository.getRateLimit()
            _uiState.update { it.copy(rateLimit = limit) }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            settingsManager.clearAuth()
            api.updateToken(null)
            _uiState.update { it.copy(authToken = "", userLogin = "", deviceLoginSuccess = false) }
            loadRateLimit()
        }
    }

    fun startDeviceLogin() {
        if (_uiState.value.deviceLoginInProgress) return
        deviceLoginJob?.cancel()
        deviceLoginJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    deviceLoginInProgress = true,
                    deviceLoginError = null,
                    deviceLoginSuccess = false,
                    deviceCode = null
                )
            }

            val deviceResp = api.requestDeviceCode()
            if (deviceResp == null || deviceResp.deviceCode.isBlank()) {
                _uiState.update {
                    it.copy(
                        deviceLoginInProgress = false,
                        deviceLoginError = "Failed to request device code"
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(deviceCode = deviceResp) }

            val interval = if (deviceResp.interval > 0) deviceResp.interval else 5
            val expiresAt = System.currentTimeMillis() + deviceResp.expiresIn * 1000L
            var pollInterval = interval

            while (System.currentTimeMillis() < expiresAt) {
                delay(pollInterval * 1000L)
                val token = api.pollAccessToken(deviceResp.deviceCode) ?: continue

                if (!token.accessToken.isNullOrBlank()) {
                    settingsManager.setAuthToken(token.accessToken)
                    api.updateToken(token.accessToken)
                    val user = repository.getCurrentUser()
                    if (user != null) settingsManager.setUserLogin(user.login)
                    _uiState.update {
                        it.copy(
                            deviceLoginInProgress = false,
                            deviceLoginSuccess = true,
                            deviceCode = null
                        )
                    }
                    loadRateLimit()
                    return@launch
                }

                when (token.error) {
                    "authorization_pending" -> { /* keep polling */ }
                    "slow_down" -> pollInterval += 5
                    "expired_token", "access_denied", "unsupported_grant_type", "incorrect_device_code" -> {
                        _uiState.update {
                            it.copy(
                                deviceLoginInProgress = false,
                                deviceLoginError = token.errorDescription ?: token.error ?: "Login failed",
                                deviceCode = null
                            )
                        }
                        return@launch
                    }
                    null -> { /* unexpected; keep polling */ }
                    else -> { /* keep polling on unknown */ }
                }
            }
            _uiState.update {
                it.copy(
                    deviceLoginInProgress = false,
                    deviceLoginError = "Login code expired",
                    deviceCode = null
                )
            }
        }
    }

    fun cancelDeviceLogin() {
        deviceLoginJob?.cancel()
        deviceLoginJob = null
        _uiState.update {
            it.copy(
                deviceLoginInProgress = false,
                deviceCode = null,
                deviceLoginError = null
            )
        }
    }

    fun consumeDeviceLoginSuccess() {
        _uiState.update { it.copy(deviceLoginSuccess = false) }
    }
}
