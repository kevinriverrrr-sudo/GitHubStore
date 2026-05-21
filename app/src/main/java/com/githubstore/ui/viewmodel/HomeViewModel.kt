package com.githubstore.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.githubstore.data.model.GithubRepo
import com.githubstore.data.repository.AppRepository
import com.githubstore.data.repository.RepositoryResult
import com.githubstore.util.FavoritesManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val repos: List<GithubRepo> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val isRateLimited: Boolean = false,
    val currentCategory: String = "trending",
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val currentPage: Int = 1,
    val hasMorePages: Boolean = true,
    val favorites: Set<String> = emptySet()
)

private const val PAGE_SIZE = 30

class HomeViewModel(
    private val repository: AppRepository,
    private val favoritesManager: FavoritesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var currentLoad: Job? = null

    init {
        loadRepos("trending")
    }

    fun loadRepos(category: String, forceRefresh: Boolean = false) {
        if (_uiState.value.isLoading && !forceRefresh) return
        currentLoad?.cancel()
        currentLoad = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    isRateLimited = false,
                    currentCategory = category,
                    searchQuery = "",
                    isSearching = false,
                    currentPage = 1,
                    repos = if (forceRefresh) it.repos else emptyList(),
                    hasMorePages = true
                )
            }

            val result = loadByCategory(category, page = 1)
            applyFirstPageResult(result)
        }
    }

    fun searchRepos(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            loadRepos(_uiState.value.currentCategory.ifBlank { "trending" })
            return
        }
        currentLoad?.cancel()
        currentLoad = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isSearching = true,
                    searchQuery = trimmed,
                    error = null,
                    isRateLimited = false,
                    currentPage = 1,
                    repos = emptyList(),
                    hasMorePages = true
                )
            }
            val result = repository.searchApps(trimmed, page = 1, perPage = PAGE_SIZE)
            applyFirstPageResult(result)
        }
    }

    fun loadMoreRepos() {
        val state = _uiState.value
        if (state.isLoading || !state.hasMorePages || state.isRateLimited) return
        currentLoad?.cancel()
        currentLoad = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val nextPage = state.currentPage + 1

            val result = if (state.isSearching && state.searchQuery.isNotBlank()) {
                repository.searchApps(state.searchQuery, page = nextPage, perPage = PAGE_SIZE)
            } else {
                loadByCategory(state.currentCategory, page = nextPage)
            }

            when (result) {
                is RepositoryResult.Success -> {
                    val merged = (state.repos + result.repos).distinctBy { it.id }
                    _uiState.update {
                        it.copy(
                            repos = merged,
                            isLoading = false,
                            currentPage = nextPage,
                            hasMorePages = result.repos.size >= PAGE_SIZE
                        )
                    }
                }
                is RepositoryResult.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
                is RepositoryResult.RateLimited -> {
                    _uiState.update { it.copy(isLoading = false, isRateLimited = true) }
                }
            }
        }
    }

    fun toggleFavorite(repoFullName: String) {
        if (repoFullName.isBlank()) return
        favoritesManager.toggleFavorite(repoFullName)
        _uiState.update { it.copy(favorites = favoritesManager.favorites) }
    }

    fun refresh() {
        loadRepos(_uiState.value.currentCategory, forceRefresh = true)
    }

    private suspend fun loadByCategory(category: String, page: Int): RepositoryResult {
        return when (category) {
            "trending" -> repository.getTrending(page = page)
            "android" -> repository.getAndroidApps(page = page)
            "desktop" -> repository.getDesktopApps(page = page)
            "linux" -> repository.getLinuxApps(page = page)
            "ios" -> repository.getIosApps(page = page)
            "all" -> repository.getAllApps(page = page)
            else -> repository.getTrending(page = page)
        }
    }

    private fun applyFirstPageResult(result: RepositoryResult) {
        when (result) {
            is RepositoryResult.Success -> {
                _uiState.update {
                    it.copy(
                        repos = result.repos.distinctBy { r -> r.id },
                        isLoading = false,
                        isRefreshing = false,
                        hasMorePages = result.repos.size >= PAGE_SIZE,
                        favorites = favoritesManager.favorites,
                        error = null
                    )
                }
            }
            is RepositoryResult.Error -> {
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = result.message) }
            }
            is RepositoryResult.RateLimited -> {
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, isRateLimited = true) }
            }
        }
    }
}
