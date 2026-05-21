package com.githubstore

import android.app.Application
import com.githubstore.data.api.GithubApi
import com.githubstore.data.repository.AppRepository
import com.githubstore.util.FavoritesManager
import com.githubstore.util.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class GitHubStoreApp : Application() {

    lateinit var githubApi: GithubApi
        private set
    lateinit var repository: AppRepository
        private set
    lateinit var favoritesManager: FavoritesManager
        private set
    lateinit var settingsManager: SettingsManager
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        settingsManager = SettingsManager(this)
        favoritesManager = FavoritesManager(this)

        // Load saved settings synchronously so first API call uses correct token/proxy
        val snapshot = runBlocking { settingsManager.snapshot() }
        githubApi = GithubApi(
            authToken = snapshot.authToken.takeIf { it.isNotBlank() },
            proxyHost = snapshot.proxyHost.takeIf { it.isNotBlank() },
            proxyPort = snapshot.proxyPort,
            proxyType = snapshot.proxyType
        )
        if (snapshot.oauthClientId.isNotBlank()) {
            githubApi.setOAuthClientId(snapshot.oauthClientId)
        }
        repository = AppRepository(githubApi)

        // Keep token in sync with settings changes
        appScope.launch {
            settingsManager.authTokenFlow.collect { token ->
                githubApi.updateToken(token.takeIf { it.isNotBlank() })
            }
        }
    }
}
