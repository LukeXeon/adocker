package com.github.andock.daemon.client

import com.github.andock.daemon.client.model.SearchResponse
import com.github.andock.daemon.client.model.SearchResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchClient @Inject constructor(
    private val client: HttpClient
) {
    /**
     * Search Docker Hub for images
     */
    suspend fun search(query: String, limit: Int = 25): Result<List<SearchResult>> =
        runCatching {
            val response = client.get("https://hub.docker.com/v2/search/repositories/") {
                parameter("query", query)
                parameter("page_size", limit)
            }
            val body = response.body<SearchResponse>()
            // Filter out invalid results (missing name field)
            body.results.filter { it.repoName != null }
        }
}