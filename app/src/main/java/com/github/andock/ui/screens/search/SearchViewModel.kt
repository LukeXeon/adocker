package com.github.andock.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.github.andock.daemon.images.ImageManager
import com.github.andock.daemon.images.downloader.ImageDownloader
import com.github.andock.daemon.search.SearchHistory
import com.github.andock.daemon.search.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Docker Hub image search.
 *
 * ## Features
 * - **Paging 3 Integration**: Exposes `Flow<PagingData<SearchResult>>` for infinite scroll
 * - **Debounced Search**: 400ms delay to reduce API calls during typing
 * - **Search History**: Persistent history with add/remove/clear operations
 * - **Advanced Filters**: Official images toggle + minimum star count
 * - **Pull Tracking**: Monitors active image downloads and progress
 *
 * ## State Management
 * - `searchQuery`: Current search input (StateFlow)
 * - `searchResults`: Paginated search results (Flow)
 * - `searchHistory`: Recent searches (StateFlow from DataStore)
 * - `activeDownloads`: Map of ongoing image pulls (StateFlow)
 *
 * ## Usage
 * ```kotlin
 * @Composable
 * fun SearchScreen(viewModel: SearchViewModel = hiltViewModel()) {
 *     val searchQuery by viewModel.searchQuery.collectAsState()
 *     val searchResults = viewModel.searchResults.collectAsLazyPagingItems()
 *
 *     TextField(
 *         value = searchQuery,
 *         onValueChange = { viewModel.updateSearchQuery(it) }
 *     )
 *
 *     LazyColumn {
 *         items(searchResults.itemCount) { index ->
 *             SearchResultCard(searchResults[index])
 *         }
 *     }
 * }
 * ```
 */
@OptIn(
    FlowPreview::class,
    ExperimentalCoroutinesApi::class
)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val searchHistoryManager: SearchHistory,
    private val imageManager: ImageManager
) : ViewModel() {

    // Search query input
    private val _searchQuery = MutableStateFlow("")

    val searchQuery = _searchQuery.asStateFlow()

    // Search history
    val searchHistory = searchHistoryManager.searchRecords
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList()
        )

    // Paginated search results with debounce
    val searchResults = _searchQuery
        .debounce(400) // 400ms debounce
        .distinctUntilChanged()
        .filter { it.isNotBlank() }
        .flatMapLatest { query ->
            searchRepository.search(query.trim())
        }.cachedIn(viewModelScope)

    /**
     * Update search query with debouncing.
     *
     * The search will be triggered automatically after 400ms of no input changes.
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Perform search and add to history.
     *
     * This method immediately sets the search query and adds it to history.
     * Use this when the user explicitly triggers a search (e.g., pressing the search button).
     */
    fun performSearch(query: String) {
        if (query.isBlank()) {
            return
        }
        _searchQuery.value = query.trim()
        viewModelScope.launch {
            searchHistoryManager.addToHistory(query.trim())
        }
    }

    /**
     * Search from history item.
     *
     * Convenience method for searching when a history item is clicked.
     */
    fun searchFromHistory(query: String) {
        performSearch(query)
    }

    /**
     * Clear search history.
     *
     * Removes all saved search queries from DataStore.
     */
    fun clearSearchHistory() {
        viewModelScope.launch {
            searchHistoryManager.clearHistory()
        }
    }

    /**
     * Remove item from search history.
     *
     * @param query The query to remove from history
     */
    fun removeFromHistory(query: String) {
        viewModelScope.launch {
            searchHistoryManager.removeFromHistory(query)
        }
    }

    /**
     * Pull an image from Docker Hub.
     *
     * Creates an [ImageDownloader] instance and tracks its progress.
     * If the image is already being downloaded, this is a no-op.
     *
     * @param imageName Full repository name (e.g., "alpine", "nginx")
     */
    fun pullImage(imageName: String) = imageManager.pullImage(imageName)

}
