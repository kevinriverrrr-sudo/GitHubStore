package com.githubstore

import android.app.Application
import com.githubstore.data.api.GithubApi
import com.githubstore.data.repository.AppRepository
import com.githubstore.util.FavoritesManager
import com.githubstore.util.SettingsManager

class GitHubStoreApp : Application() {

    lateinit var githubApi: GithubApi
        private set
    lateinit var repository: AppRepository
        private set
    lateinit var favoritesManager: FavoritesManager
        private set
    lateinit var settingsManager: SettingsManager
        private set

    override fun onCreate() {
        super.onCreate()

        settingsManager = SettingsManager(this)
        favoritesManager = FavoritesManager(this)
        githubApi = GithubApi()
        repository = AppRepository(githubApi)
    }
}
