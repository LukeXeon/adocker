package com.github.andock.daemon.search.model

import kotlinx.serialization.Serializable

/**
 * Docker Hub Search Response
 *
 * Response from the Docker Hub Search API (not part of Registry API V2).
 * Provides paginated search results for container images.
 * API Endpoint: https://hub.docker.com/v2/search/repositories/
 *
 * @property count Total number of search results
 * @property next URL for the next page of results (pagination)
 * @property previous URL for the previous page of results (pagination)
 * @property results List of search result items
 */
@Serializable
data class SearchResponse(
    val count: Int,
    val next: String? = null,
    val previous: String? = null,
    val results: List<SearchResult>
)