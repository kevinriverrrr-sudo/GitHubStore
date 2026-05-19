package com.githubstore.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        private val THEME_KEY = stringPreferencesKey("theme")
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val PROXY_HOST_KEY = stringPreferencesKey("proxy_host")
        private val PROXY_PORT_KEY = intPreferencesKey("proxy_port")
        private val DOWNLOAD_DIR_KEY = stringPreferencesKey("download_dir")
        private val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
    }

    val themeFlow: Flow<String> = context.dataStore.data.map { it[THEME_KEY] ?: "system" }
    val languageFlow: Flow<String> = context.dataStore.data.map { it[LANGUAGE_KEY] ?: "en" }
    val proxyHostFlow: Flow<String> = context.dataStore.data.map { it[PROXY_HOST_KEY] ?: "" }
    val proxyPortFlow: Flow<Int> = context.dataStore.data.map { it[PROXY_PORT_KEY] ?: 0 }
    val downloadDirFlow: Flow<String> = context.dataStore.data.map { it[DOWNLOAD_DIR_KEY] ?: "" }
    val authTokenFlow: Flow<String> = context.dataStore.data.map { it[AUTH_TOKEN_KEY] ?: "" }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { it[THEME_KEY] = theme }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { it[LANGUAGE_KEY] = language }
    }

    suspend fun setProxy(host: String, port: Int) {
        context.dataStore.edit {
            it[PROXY_HOST_KEY] = host
            it[PROXY_PORT_KEY] = port
        }
    }

    suspend fun setDownloadDir(dir: String) {
        context.dataStore.edit { it[DOWNLOAD_DIR_KEY] = dir }
    }

    suspend fun setAuthToken(token: String) {
        context.dataStore.edit { it[AUTH_TOKEN_KEY] = token }
    }

    suspend fun clearProxy() {
        context.dataStore.edit {
            it.remove(PROXY_HOST_KEY)
            it.remove(PROXY_PORT_KEY)
        }
    }
}
