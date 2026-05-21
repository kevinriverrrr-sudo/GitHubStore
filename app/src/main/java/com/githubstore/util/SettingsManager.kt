package com.githubstore.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        private val THEME_KEY = stringPreferencesKey("theme")
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val PROXY_HOST_KEY = stringPreferencesKey("proxy_host")
        private val PROXY_PORT_KEY = intPreferencesKey("proxy_port")
        private val PROXY_TYPE_KEY = stringPreferencesKey("proxy_type")
        private val DOWNLOAD_DIR_KEY = stringPreferencesKey("download_dir")
        private val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
        private val OAUTH_CLIENT_ID_KEY = stringPreferencesKey("oauth_client_id")
        private val USER_LOGIN_KEY = stringPreferencesKey("user_login")
    }

    val themeFlow: Flow<String> = context.dataStore.data.map { it[THEME_KEY] ?: "system" }
    val languageFlow: Flow<String> = context.dataStore.data.map { it[LANGUAGE_KEY] ?: "en" }
    val proxyHostFlow: Flow<String> = context.dataStore.data.map { it[PROXY_HOST_KEY] ?: "" }
    val proxyPortFlow: Flow<Int> = context.dataStore.data.map { it[PROXY_PORT_KEY] ?: 0 }
    val proxyTypeFlow: Flow<String> = context.dataStore.data.map { it[PROXY_TYPE_KEY] ?: "http" }
    val downloadDirFlow: Flow<String> = context.dataStore.data.map { it[DOWNLOAD_DIR_KEY] ?: "" }
    val authTokenFlow: Flow<String> = context.dataStore.data.map { it[AUTH_TOKEN_KEY] ?: "" }
    val oauthClientIdFlow: Flow<String> = context.dataStore.data.map { it[OAUTH_CLIENT_ID_KEY] ?: "" }
    val userLoginFlow: Flow<String> = context.dataStore.data.map { it[USER_LOGIN_KEY] ?: "" }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { it[THEME_KEY] = theme }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { it[LANGUAGE_KEY] = language }
    }

    suspend fun setProxy(host: String, port: Int, type: String = "http") {
        context.dataStore.edit {
            it[PROXY_HOST_KEY] = host
            it[PROXY_PORT_KEY] = port
            it[PROXY_TYPE_KEY] = type
        }
    }

    suspend fun setDownloadDir(dir: String) {
        context.dataStore.edit { it[DOWNLOAD_DIR_KEY] = dir }
    }

    suspend fun setAuthToken(token: String) {
        context.dataStore.edit { it[AUTH_TOKEN_KEY] = token }
    }

    suspend fun setOAuthClientId(clientId: String) {
        context.dataStore.edit { it[OAUTH_CLIENT_ID_KEY] = clientId }
    }

    suspend fun setUserLogin(login: String) {
        context.dataStore.edit { it[USER_LOGIN_KEY] = login }
    }

    suspend fun clearProxy() {
        context.dataStore.edit {
            it.remove(PROXY_HOST_KEY)
            it.remove(PROXY_PORT_KEY)
            it.remove(PROXY_TYPE_KEY)
        }
    }

    suspend fun clearAuth() {
        context.dataStore.edit {
            it.remove(AUTH_TOKEN_KEY)
            it.remove(USER_LOGIN_KEY)
        }
    }

    // Synchronous reads of initial values (called on app startup)
    suspend fun snapshot(): SettingsSnapshot {
        val prefs = context.dataStore.data.first()
        return SettingsSnapshot(
            theme = prefs[THEME_KEY] ?: "system",
            language = prefs[LANGUAGE_KEY] ?: "en",
            proxyHost = prefs[PROXY_HOST_KEY] ?: "",
            proxyPort = prefs[PROXY_PORT_KEY] ?: 0,
            proxyType = prefs[PROXY_TYPE_KEY] ?: "http",
            authToken = prefs[AUTH_TOKEN_KEY] ?: "",
            oauthClientId = prefs[OAUTH_CLIENT_ID_KEY] ?: "",
            userLogin = prefs[USER_LOGIN_KEY] ?: ""
        )
    }
}

data class SettingsSnapshot(
    val theme: String,
    val language: String,
    val proxyHost: String,
    val proxyPort: Int,
    val proxyType: String,
    val authToken: String,
    val oauthClientId: String,
    val userLogin: String
)
