package com.github.andock.daemon.search

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.github.andock.daemon.search.model.SearchResult
import io.ktor.http.URLBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
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
 * @see SearchPagingSource
 */
@Singleton
class SearchRepository @Inject constructor(
    private val factory: SearchPagingSource.Factory,
    private val json: Json,
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
     * @return Flow of PagingData containing search results
     */
    fun search(parameters: SearchParameters): Flow<PagingData<SearchResult>> {
        val jsonObject = json.encodeToJsonElement(parameters) as JsonObject
        return Pager(
            config = PagingConfig(
                pageSize = parameters.pageSize,
                enablePlaceholders = false,
                initialLoadSize = parameters.pageSize,
            ),
            initialKey = SearchKey(
                URLBuilder("https://hub.docker.com/v2/search/repositories/")
                    .also {
                        it.parameters.apply {
                            jsonObject.asSequence().forEach { (k, v) ->
                                when (v) {
                                    is JsonPrimitive -> {
                                        append(k, v.toString())
                                    }

                                    is JsonArray -> {
                                        appendAll(
                                            k,
                                            v.asSequence().map { v -> v.toString() }.asIterable()
                                        )
                                    }

                                    else -> Unit
                                }

                            }
                        }
                    }
                    .build(),
                null,
            ),
            pagingSourceFactory = factory
        ).flow
    }
}