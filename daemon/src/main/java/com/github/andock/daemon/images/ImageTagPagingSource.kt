package com.github.andock.daemon.images

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.andock.daemon.images.model.TagsListResponse
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Singleton

class ImageTagPagingSource @AssistedInject constructor(
    private val client: HttpClient,
    private val imageClient: ImageClient,
) : PagingSource<ImageTagParameters, String>() {

    override fun getRefreshKey(
        state: PagingState<ImageTagParameters, String>
    ): ImageTagParameters? = null

    override suspend fun load(params: LoadParams<ImageTagParameters>): LoadResult<ImageTagParameters, String> {
        val key = params.key ?: return LoadResult.Page(emptyList(), null, null)
        val (registry, repository, last) = key
        return withContext(Dispatchers.IO) {
            imageClient.authenticate(repository, registry)
                .mapCatching { authToken ->
                    client.get("$registry/v2/${repository}/tags/list") {
                        if (authToken.isNotEmpty()) {
                            header(HttpHeaders.Authorization, "Bearer $authToken")
                        }
                        if (!last.isNullOrEmpty()) {
                            parameter("last", last)
                        }
                    }.body<TagsListResponse>().tags
                }.fold(
                    {
                        if (it.isEmpty()) {
                            LoadResult.Page(
                                data = emptyList(),
                                prevKey = null,
                                nextKey = null
                            )
                        } else {
                            LoadResult.Page(
                                data = it,
                                prevKey = null,
                                nextKey = key.copy(last = it.lastOrNull())
                            )
                        }
                    },
                    {
                        LoadResult.Error(it)
                    }
                )
        }
    }

    @Singleton
    @AssistedFactory
    interface Factory : () -> ImageTagPagingSource {
        override fun invoke(): ImageTagPagingSource
    }
}