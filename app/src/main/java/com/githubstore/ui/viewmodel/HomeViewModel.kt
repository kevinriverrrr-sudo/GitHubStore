package com.githubstore.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.githubstore.data.api.GithubApi
import com.githubstore.data.model.GithubRepo
import com.githubstore.data.repository.AppRepository
import com.githubstore.data.repository.RepositoryResult
import com.githubstore.util.FavoritesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val repos: List<GithubRepo> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val isRateLimited: Boolean = false,
    val currentCategory: String = "trending",
    val searchQuery: String = "",
    val currentPage: Int = 1,
    val hasMorePages: Boolean = true,
    val favorites: Set<String> = emptySet()
)

class HomeViewModel(
    private val repository: AppRepository,
    private val favoritesManager: FavoritesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadRepos(category = "trending")
    }

    fun loadRepos(category: String, forceRefresh: Boolean = false) {
        val state = _uiState.value
        if (state.isLoading && !forceRefresh) return

        viewModelScope.launch {
            _uiState.value = state.copy(
                isLoading = true,
                error = null,
                isRateLimited = false,
                currentCategory = category,
                currentPage = 1
            )

            val result = when (category) {
                "trending" -> repository.getTrending(page = 1)
                "android" -> repository.getAndroidApps(page = 1)
                "desktop" -> repository.getDesktopApps(page = 1)
                "linux" -> repository.getLinuxApps(page = 1)
                "ios" -> repository.getIosApps(page = 1)
                "all" -> repository.getAllApps(page = 1)
                else -> repository.getTrending(page = 1)
            }

            when (result) {
                is RepositoryResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        repos = result.repos,
                        isLoading = false,
                        hasMorePages = result.repos.size >= 30,
                        favorites = favoritesManager.favorites
                    )
                }
                is RepositoryResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is RepositoryResult.RateLimited -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRateLimited = true
                    )
                }
            }
        }
    }

    fun searchRepos(query: String) {
        if (query.isBlank()) {
            loadRepos(_uiState.value.currentCategory)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                searchQuery = query,
                error = null
            )
            val result = repository.searchApps(query)
            when (result) {
                is RepositoryResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        repos = result.repos,
                        isLoading = false,
                        hasMorePages = result.repos.size >= 30
                    )
                }
                is RepositoryResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is RepositoryResult.RateLimited -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRateLimited = true
                    )
                }
            }
        }
    }

    fun loadMoreRepos() {
        val state = _uiState.value
        if (state.isLoading || !state.hasMorePages || state.isRateLimited) return

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true)
            val nextPage = state.currentPage + 1

            val result = when {
                state.searchQuery.isNotBlank() -> repository.searchApps(state.searchQuery, page = nextPage)
                else -> when (state.currentCategory) {
                    "android" -> repository.getAndroidApps(page = nextPage)
                    "desktop" -> repository.getDesktopApps(page = nextPage)
                    "linux" -> repository.getLinuxApps(page = nextPage)
                    "ios" -> repository.getIosApps(page = nextPage)
                    "all" -> repository.getAllApps(page = nextPage)
                    else -> repository.getTrending(page = nextPage)
                }
            }

            when (result) {
                is RepositoryResult.Success -> {
                    val newRepos = state.repos + result.repos
                    _uiState.value = _uiState.value.copy(
                        repos = newRepos,
                        isLoading = false,
                        currentPage = nextPage,
                        hasMorePages = result.repos.size >= 30
                    )
                }
                is RepositoryResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                is RepositoryResult.RateLimited -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRateLimited = true
                    )
                }
            }
        }
    }

    fun toggleFavorite(repoFullName: String) {
        val isNowFavorite = favoritesManager.toggleFavorite(repoFullName)
        val newFavorites = favoritesManager.favorites
        _uiState.value = _uiState.value.copy(favorites = newFavorites)
    }

    fun refresh() {
        val state = _uiState.value
        _uiState.value = state.copy(isRefreshing = true)
        loadRepos(state.currentCategory, forceRefresh = true)
        _uiState.value = _uiState.value.copy(isRefreshing = false)
    }
}
