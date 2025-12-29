package com.github.andock.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.github.andock.daemon.images.ImageManager
import com.github.andock.daemon.images.downloader.ImageDownloadState
import com.github.andock.daemon.images.downloader.ImageDownloader
import com.github.andock.daemon.search.SearchHistory
import com.github.andock.daemon.search.SearchRepository
import com.github.andock.daemon.search.model.SearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filter states
    private val _showOnlyOfficial = MutableStateFlow(false)
    val showOnlyOfficial: StateFlow<Boolean> = _showOnlyOfficial.asStateFlow()

    private val _minStars = MutableStateFlow(0)
    val minStars: StateFlow<Int> = _minStars.asStateFlow()

    // Search history
    val searchHistory: StateFlow<List<String>> = searchHistoryManager.searchRecords
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Paginated search results with debounce
    val searchResults: Flow<PagingData<SearchResult>> = _searchQuery
        .debounce(400) // 400ms debounce
        .distinctUntilChanged()
        .filter { it.isNotBlank() }
        .flatMapLatest { query ->
            searchRepository.search(query.trim())
        }
        .cachedIn(viewModelScope)

    // Active image downloads (repoName -> ImageDownloader)
    private val _activeDownloads = MutableStateFlow<Map<String, ImageDownloader>>(emptyMap())
    val activeDownloads: StateFlow<Map<String, ImageDownloader>> = _activeDownloads.asStateFlow()

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
        if (query.isBlank()) return

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
     * Toggle official images filter.
     *
     * When enabled, only official Docker images will be shown in the UI.
     * Note: This filter is applied in the UI layer, not in the PagingSource.
     */
    fun toggleOfficialFilter() {
        _showOnlyOfficial.value = !_showOnlyOfficial.value
    }

    /**
     * Update minimum stars filter.
     *
     * @param stars Minimum number of stars (will be coerced to >= 0)
     */
    fun updateMinStars(stars: Int) {
        _minStars.value = stars.coerceAtLeast(0)
    }

    /**
     * Pull an image from Docker Hub.
     *
     * Creates an [ImageDownloader] instance and tracks its progress.
     * If the image is already being downloaded, this is a no-op.
     *
     * @param imageName Full repository name (e.g., "alpine", "nginx")
     */
    fun pullImage(imageName: String) {
        if (_activeDownloads.value.containsKey(imageName)) {
            return // Already downloading
        }

        val downloader = imageManager.pullImage(imageName)

        _activeDownloads.value += (imageName to downloader)

        // Monitor download completion
        viewModelScope.launch {
            downloader.state.collect { state ->
                if (state is ImageDownloadState.Done || state is ImageDownloadState.Error) {
                    _activeDownloads.value -= imageName
                }
            }
        }
    }

    /**
     * Cancel an active image download.
     *
     * @param imageName The repository name of the image to cancel
     */
    fun cancelDownload(imageName: String) {
        _activeDownloads.value[imageName]?.cancel()
        _activeDownloads.value -= imageName
    }

    /**
     * Check if an image is currently being downloaded.
     *
     * @param imageName The repository name to check
     * @return True if the image is being downloaded
     */
    fun isDownloading(imageName: String): Boolean {
        return _activeDownloads.value.containsKey(imageName)
    }

    /**
     * Get the download state for an image.
     *
     * @param imageName The repository name
     * @return The download state, or null if not downloading
     */
    fun getDownloadState(imageName: String): ImageDownloadState? {
        return _activeDownloads.value[imageName]?.state?.value
    }
}
