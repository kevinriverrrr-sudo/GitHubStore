package com.githubstore.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.githubstore.data.api.GithubApi
import com.githubstore.data.model.ApiRateLimit
import com.githubstore.data.repository.AppRepository
import com.githubstore.util.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val theme: String = "system",
    val language: String = "en",
    val proxyHost: String = "",
    val proxyPort: Int = 0,
    val downloadDir: String = "",
    val authToken: String = "",
    val rateLimit: ApiRateLimit? = null,
    val isProxyEnabled: Boolean = false
)

class SettingsViewModel(
    private val settingsManager: SettingsManager,
    private val repository: AppRepository,
    private val api: GithubApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsManager.themeFlow.collect { theme ->
                _uiState.value = _uiState.value.copy(theme = theme)
            }
        }
        viewModelScope.launch {
            settingsManager.languageFlow.collect { lang ->
                _uiState.value = _uiState.value.copy(language = lang)
            }
        }
        viewModelScope.launch {
            settingsManager.proxyHostFlow.collect { host ->
                _uiState.value = _uiState.value.copy(proxyHost = host, isProxyEnabled = host.isNotBlank())
            }
        }
        viewModelScope.launch {
            settingsManager.proxyPortFlow.collect { port ->
                _uiState.value = _uiState.value.copy(proxyPort = port)
            }
        }
        viewModelScope.launch {
            settingsManager.downloadDirFlow.collect { dir ->
                _uiState.value = _uiState.value.copy(downloadDir = dir)
            }
        }
        viewModelScope.launch {
            settingsManager.authTokenFlow.collect { token ->
                _uiState.value = _uiState.value.copy(authToken = token)
            }
        }
        loadRateLimit()
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            settingsManager.setTheme(theme)
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            settingsManager.setLanguage(language)
        }
    }

    fun setProxy(host: String, port: Int) {
        viewModelScope.launch {
            if (host.isBlank()) {
                settingsManager.clearProxy()
                api.updateProxy(null, 0)
            } else {
                settingsManager.setProxy(host, port)
                api.updateProxy(host, port)
            }
        }
    }

    fun setDownloadDir(dir: String) {
        viewModelScope.launch {
            settingsManager.setDownloadDir(dir)
        }
    }

    fun setAuthToken(token: String) {
        viewModelScope.launch {
            settingsManager.setAuthToken(token)
            if (token.isNotBlank()) {
                api.updateToken(token)
            } else {
                api.updateToken(null)
            }
        }
    }

    fun loadRateLimit() {
        viewModelScope.launch {
            val limit = repository.getRateLimit()
            _uiState.value = _uiState.value.copy(rateLimit = limit)
        }
    }
}
