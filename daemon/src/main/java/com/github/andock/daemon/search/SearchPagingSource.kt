package com.github.andock.daemon.search

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.andock.daemon.search.model.SearchResult
import timber.log.Timber

/**
 * PagingSource for Docker Hub search results.
 *
 * Implements URL-based pagination by following the `next` URLs returned by the API.
 * This is the recommended approach for Docker Hub's undocumented search API, which
 * returns full URLs for pagination rather than page numbers.
 *
 * ## Load Behavior
 * - **Initial Load**: Uses [SearchClient.search] with query parameters when `params.key` is null
 * - **Subsequent Loads**: Uses [SearchClient.searchByUrl] with the URL from `params.key`
 * - **Refresh**: Always starts from the beginning (returns `null` in [getRefreshKey])
 *
 * ## Error Handling
 * Network errors and API failures are wrapped in [LoadResult.Error] and surfaced to the UI
 * via `loadState` in `LazyPagingItems`.
 *
 * @param query The search query string (e.g., "alpine", "nginx")
 * @param pageSize Number of results per page (default: 25, max: 100)
 * @param searchClient Client for making Docker Hub API requests
 *
 * @see SearchRepository
 * @see SearchClient
 */
class SearchPagingSource(
    private val query: String,
    private val pageSize: Int = 25,
    private val searchClient: SearchClient
) : PagingSource<String, SearchResult>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, SearchResult> {
        return try {
            val response = if (params.key == null) {
                // Initial load - use query-based search
                searchClient.search(query, pageSize).getOrThrow()
            } else {
                // Subsequent loads - use next URL
                searchClient.searchByUrl(params.key!!).getOrThrow()
            }

            LoadResult.Page(
                data = response.results.filter { it.repoName != null },
                prevKey = response.previous,
                nextKey = response.next
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to load search results for query: $query")
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<String, SearchResult>): String? {
        // Return null to always start from the beginning on refresh
        // This ensures search results are always current and consistent
        return null
    }
}
