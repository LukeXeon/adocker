package com.github.andock.daemon.search

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.andock.daemon.search.model.SearchResponse
import com.github.andock.daemon.search.model.SearchResult
import com.google.common.hash.BloomFilter
import com.google.common.hash.Funnels
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Singleton

/**
 * PagingSource for Docker Hub search results.
 *
 * Implements URL-based pagination by following the `next` URLs returned by the API.
 * This is the recommended approach for Docker Hub's undocumented search API, which
 * returns full URLs for pagination rather than page numbers.
 *
 *
 * @see SearchRepository
 */
class SearchPagingSource @AssistedInject constructor(
    private val client: HttpClient
) : PagingSource<SearchPagingKey, SearchResult>() {

    @Suppress("UnstableApiUsage")
    override suspend fun load(params: LoadParams<SearchPagingKey>): LoadResult<SearchPagingKey, SearchResult> {
        val key = params.key ?: return LoadResult.Page(
            emptyList(),
            null,
            null
        )
        return withContext(Dispatchers.IO) {
            runCatching {
                client.get(key.url).body<SearchResponse>()
            }.mapCatching { response ->
                val names = key.names?.copy() ?: BloomFilter.create(
                    Funnels.stringFunnel(Charsets.UTF_8), // 数据类型转换器（支持 String、Int 等）
                    100000,
                    0.01
                )
                LoadResult.Page(
                    data = response.results.filter {
                        it.repoName != null && names.put(it.repoName)
                    },
                    prevKey = if (!response.previous.isNullOrBlank()) {
                        SearchPagingKey(
                            Url(response.previous),
                            names,
                        )
                    } else {
                        null
                    },
                    nextKey = if (!response.next.isNullOrBlank()) {
                        SearchPagingKey(
                            Url(response.next),
                            names
                        )
                    } else {
                        null
                    }
                )
            }.fold(
                { response ->
                    response
                },
                { e ->
                    Timber.e(e)
                    LoadResult.Error(e)
                }
            )
        }
    }

    override fun getRefreshKey(state: PagingState<SearchPagingKey, SearchResult>): SearchPagingKey? {
        return null
    }

    @Singleton
    @AssistedFactory
    interface Factory : () -> SearchPagingSource {
        override operator fun invoke(): SearchPagingSource
    }
}
