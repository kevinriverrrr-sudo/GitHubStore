package com.githubstore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.githubstore.ui.screens.*
import com.githubstore.ui.theme.GitHubStoreTheme
import com.githubstore.ui.viewmodel.*

class MainActivity : ComponentActivity() {

    private lateinit var app: GitHubStoreApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        app = application as GitHubStoreApp

        setContent {
            var themeSetting by remember { mutableStateOf("system") }

            LaunchedEffect(Unit) {
                app.settingsManager.themeFlow.collect { theme ->
                    themeSetting = theme
                }
            }

            val isDarkTheme = when (themeSetting) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            GitHubStoreTheme(
                darkTheme = isDarkTheme,
                dynamicColor = true
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            val viewModel: HomeViewModel = viewModel(
                                factory = HomeViewModelFactory(app.repository, app.favoritesManager)
                            )
                            HomeScreen(
                                viewModel = viewModel,
                                onRepoClick = { owner, repo ->
                                    navController.navigate("detail/$owner/$repo")
                                },
                                onFavoritesClick = {
                                    navController.navigate("favorites")
                                },
                                onSettingsClick = {
                                    navController.navigate("settings")
                                }
                            )
                        }

                        composable(
                            route = "detail/{owner}/{repo}",
                            arguments = listOf(
                                navArgument("owner") { type = NavType.StringType },
                                navArgument("repo") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val owner = backStackEntry.arguments?.getString("owner") ?: ""
                            val repo = backStackEntry.arguments?.getString("repo") ?: ""
                            val viewModel: DetailViewModel = viewModel(
                                factory = DetailViewModelFactory(app.repository, app.favoritesManager)
                            )
                            LaunchedEffect(owner, repo) {
                                viewModel.loadRepoDetails(owner, repo)
                            }
                            DetailScreen(
                                viewModel = viewModel,
                                owner = owner,
                                repoName = repo,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("favorites") {
                            val viewModel: FavoritesViewModel = viewModel(
                                factory = FavoritesViewModelFactory(app.repository, app.favoritesManager)
                            )
                            LaunchedEffect(Unit) {
                                viewModel.loadFavorites()
                            }
                            FavoritesScreen(
                                viewModel = viewModel,
                                onRepoClick = { owner, repo ->
                                    navController.navigate("detail/$owner/$repo")
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("settings") {
                            val viewModel: SettingsViewModel = viewModel(
                                factory = SettingsViewModelFactory(app.settingsManager, app.repository, app.githubApi)
                            )
                            SettingsScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ViewModel Factories for proper lifecycle management
class HomeViewModelFactory(
    private val repository: com.githubstore.data.repository.AppRepository,
    private val favoritesManager: com.githubstore.util.FavoritesManager
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(repository, favoritesManager) as T
    }
}

class DetailViewModelFactory(
    private val repository: com.githubstore.data.repository.AppRepository,
    private val favoritesManager: com.githubstore.util.FavoritesManager
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DetailViewModel(repository, favoritesManager) as T
    }
}

class FavoritesViewModelFactory(
    private val repository: com.githubstore.data.repository.AppRepository,
    private val favoritesManager: com.githubstore.util.FavoritesManager
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return FavoritesViewModel(repository, favoritesManager) as T
    }
}

class SettingsViewModelFactory(
    private val settingsManager: com.githubstore.util.SettingsManager,
    private val repository: com.githubstore.data.repository.AppRepository,
    private val api: com.githubstore.data.api.GithubApi
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SettingsViewModel(settingsManager, repository, api) as T
    }
}
