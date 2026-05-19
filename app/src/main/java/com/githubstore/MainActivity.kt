package com.githubstore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
            var isDarkTheme by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                app.settingsManager.themeFlow.collect { theme ->
                    isDarkTheme = when (theme) {
                        "dark" -> true
                        "light" -> false
                        else -> false // system default handled by isSystemInDarkTheme
                    }
                }
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
                            HomeScreen(
                                viewModel = HomeViewModel(app.repository, app.favoritesManager),
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
                            DetailScreen(
                                viewModel = DetailViewModel(app.repository, app.favoritesManager),
                                owner = owner,
                                repoName = repo,
                                downloadDir = "", // Will be fetched from settings
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("favorites") {
                            FavoritesScreen(
                                viewModel = FavoritesViewModel(app.repository, app.favoritesManager),
                                onRepoClick = { owner, repo ->
                                    navController.navigate("detail/$owner/$repo")
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                viewModel = SettingsViewModel(app.settingsManager, app.repository, app.githubApi),
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
