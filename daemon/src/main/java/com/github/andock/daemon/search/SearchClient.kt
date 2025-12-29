package com.github.andock.daemon.search

import com.github.andock.daemon.search.model.SearchResponse
import com.github.andock.daemon.search.model.SearchResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Client for Docker Hub Search API.
 *
 * Provides access to Docker Hub's image search functionality with support
 * for both query-based and URL-based pagination.
 */
@Singleton
class SearchClient @Inject constructor(
    private val client: HttpClient
) {
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
    suspend fun search(query: String, limit: Int = 25): Result<SearchResponse> =
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
    suspend fun searchByUrl(url: String): Result<SearchResponse> =
        runCatching {
            val response = client.get(url)
            response.body<SearchResponse>()
        }
}