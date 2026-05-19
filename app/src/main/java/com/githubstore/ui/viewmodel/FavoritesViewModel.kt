package com.githubstore.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.githubstore.data.model.GithubRepo
import com.githubstore.data.repository.AppRepository
import com.githubstore.util.FavoritesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val favoriteRepos: List<GithubRepo> = emptyList(),
    val isLoading: Boolean = false,
    val isEmpty: Boolean = true
)

class FavoritesViewModel(
    private val repository: AppRepository,
    private val favoritesManager: FavoritesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    fun loadFavorites() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val favoriteNames = favoritesManager.favorites
            val repos = mutableListOf<GithubRepo>()

            for (fullName in favoriteNames) {
                val parts = fullName.split("/")
                if (parts.size == 2) {
                    val repo = repository.getRepoDetails(parts[0], parts[1])
                    if (repo != null) {
                        repos.add(repo)
                    }
                }
            }

            _uiState.value = _uiState.value.copy(
                favoriteRepos = repos,
                isLoading = false,
                isEmpty = repos.isEmpty()
            )
        }
    }

    fun removeFavorite(fullName: String) {
        favoritesManager.toggleFavorite(fullName)
        loadFavorites()
    }
}
