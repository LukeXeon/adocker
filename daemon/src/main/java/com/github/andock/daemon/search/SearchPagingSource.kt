package com.github.andock.daemon.search

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.andock.daemon.search.model.SearchResponse
import com.github.andock.daemon.search.model.SearchResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import timber.log.Timber
import javax.inject.Singleton

/**
 * PagingSource for Docker Hub search results.
 *
 * Implements URL-based pagination by following the `next` URLs returned by the API.
 * This is the recommended approach for Docker Hub's undocumented search API, which
 * returns full URLs for pagination rather than page numbers.
 *
 * ## Load Behavior
 * - **Initial Load**: Uses [search] with query parameters when `params.key` is null
 * - **Subsequent Loads**: Uses [searchByUrl] with the URL from `params.key`
 * - **Refresh**: Always starts from the beginning (returns `null` in [getRefreshKey])
 *
 * ## Error Handling
 * Network errors and API failures are wrapped in [LoadResult.Error] and surfaced to the UI
 * via `loadState` in `LazyPagingItems`.
 *
 * @param query The search query string (e.g., "alpine", "nginx")
 * @param pageSize Number of results per page (default: 25, max: 100)
 *
 * @see SearchRepository
 */
class SearchPagingSource @AssistedInject constructor(
    @Assisted
    private val query: String,
    @Assisted
    private val pageSize: Int = 25,
    private val client: HttpClient
) : PagingSource<String, SearchResult>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, SearchResult> {
        return try {
            val response = if (params.key == null) {
                // Initial load - use query-based search
                search(query, pageSize).getOrThrow()
            } else {
                // Subsequent loads - use next URL
                searchByUrl(params.key!!).getOrThrow()
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


    /**
     * Search Docker Hub for images using a query string.
     *
     * This is the initial search method that should be used for the first page of results.
     * Subsequent pages can be fetched using [searchByUrl] with the `next` URL from the response.
     *
     * @param query Search query string (e.g., "alpine", "nginx")
     * @param limit Number of results per page (default: 25, max: 100)
     * @return Result containing the full [SearchResponse] with pagination URLs
     */
    private suspend fun search(query: String, limit: Int = 25): Result<SearchResponse> =
        runCatching {
            val response = client.get("https://hub.docker.com/v2/search/repositories/") {
                parameter("query", query)
                parameter("page_size", limit)
            }
            response.body<SearchResponse>()
        }

    /**
     * Fetch search results using a full URL (for pagination).
     *
     * This method is used to follow the `next` or `previous` URLs returned
     * in a [SearchResponse] to navigate through paginated results.
     *
     * @param url Full URL to fetch (from SearchResponse.next or SearchResponse.previous)
     * @return Result containing the [SearchResponse] for that page
     */
    private suspend fun searchByUrl(url: String): Result<SearchResponse> =
        runCatching {
            val response = client.get(url)
            response.body<SearchResponse>()
        }

    @Singleton
    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted
            query: String,
            @Assisted
            pageSize: Int = 25,
        ): SearchPagingSource
    }
}
