package com.github.andock.daemon.search

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.github.andock.daemon.search.model.SearchResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Docker Hub image search.
 *
 * Provides paginated search results using the Paging 3 library with URL-based pagination.
 * Each search query creates a new [Pager] instance that manages the pagination state.
 *
 * ## Usage
 * ```kotlin
 * val searchResults: Flow<PagingData<SearchResult>> = searchRepository.searchImages("alpine")
 *     .cachedIn(viewModelScope) // Cache to survive configuration changes
 * ```
 *
 * @param searchClient Client for Docker Hub search API
 *
 * @see SearchPagingSource
 * @see SearchClient
 */
@Singleton
class SearchRepository @Inject constructor(
    private val searchClient: SearchClient
) {
    /**
     * Search Docker Hub for images with pagination.
     *
     * Creates a new paging stream for the given query. The stream will automatically
     * load additional pages as the user scrolls through results.
     *
     * **Important**: Always use `.cachedIn(scope)` on the returned Flow to cache
     * the PagingData and survive configuration changes.
     *
     * @param query Search query string (e.g., "alpine", "nginx")
     * @param pageSize Number of results per page (default: 25, max: 100)
     * @return Flow of PagingData containing search results
     */
    fun searchImages(query: String, pageSize: Int = 25): Flow<PagingData<SearchResult>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false,
                initialLoadSize = pageSize
            ),
            pagingSourceFactory = {
                SearchPagingSource(query, pageSize, searchClient)
            }
        ).flow
    }
}
