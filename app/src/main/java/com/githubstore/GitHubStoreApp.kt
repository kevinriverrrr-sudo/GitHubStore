package com.githubstore

import android.app.Application
import coil.Coil
import coil.ImageLoader
import com.githubstore.data.api.GithubApi
import com.githubstore.data.repository.AppRepository
import com.githubstore.util.FavoritesManager
import com.githubstore.util.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

class GitHubStoreApp : Application() {

    lateinit var githubApi: GithubApi
        private set
    lateinit var repository: AppRepository
        private set
    lateinit var favoritesManager: FavoritesManager
        private set
    lateinit var settingsManager: SettingsManager
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()

        settingsManager = SettingsManager(this)
        favoritesManager = FavoritesManager(this)

        // Short, bounded read so the very first request uses the saved token/proxy.
        val initial = runBlocking {
            withTimeoutOrNull(200) { settingsManager.snapshot() }
        }
        githubApi = GithubApi(
            authToken = initial?.authToken?.takeIf { it.isNotBlank() },
            proxyHost = initial?.proxyHost?.takeIf { it.isNotBlank() },
            proxyPort = if ((initial?.proxyPort ?: 0) in 1..65535) initial?.proxyPort ?: 0 else 0,
            proxyType = initial?.proxyType ?: "http"
        )
        initial?.oauthClientId?.takeIf { it.isNotBlank() }?.let {
            githubApi.setOAuthClientId(it)
        }
        repository = AppRepository(githubApi)

        // Make Coil share the configured OkHttp client so image loads honor proxy settings.
        rebuildCoil()

        // Reactive sync for changes after startup.
        appScope.launch {
            settingsManager.authTokenFlow.collect { token ->
                githubApi.updateToken(token.takeIf { it.isNotBlank() })
            }
        }
        appScope.launch {
            combine(
                settingsManager.proxyHostFlow,
                settingsManager.proxyPortFlow,
                settingsManager.proxyTypeFlow
            ) { host, port, type -> Triple(host, port, type) }
                .collect { (host, port, type) ->
                    val safeHost = host.takeIf { it.isNotBlank() }
                    val safePort = if (port in 1..65535) port else 0
                    githubApi.updateProxy(safeHost, safePort, type)
                    // Rebuild Coil so subsequent image loads use the new proxy.
                    rebuildCoil()
                }
        }
        appScope.launch {
            settingsManager.oauthClientIdFlow.collect { clientId ->
                if (clientId.isNotBlank()) githubApi.setOAuthClientId(clientId)
            }
        }
    }

    private fun rebuildCoil() {
        val loader = ImageLoader.Builder(this)
            .okHttpClient { githubApi.client }
            .crossfade(true)
            .build()
        Coil.setImageLoader(loader)
    }
}
