package com.github.andock.daemon.images

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.andock.daemon.images.model.TagsListResponse
import com.github.andock.daemon.registries.RegistryModule
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
) : PagingSource<Pair<String, String?>, String>() {

    companion object {
        const val N = 100
    }

    override fun getRefreshKey(
        state: PagingState<Pair<String, String?>, String>
    ): Pair<String, String?>? = null

    override suspend fun load(params: LoadParams<Pair<String, String?>>): LoadResult<Pair<String, String?>, String> {
        val key = params.key ?: return LoadResult.Page(emptyList(), null, null)
        val (repository, last) = key
        val registry = RegistryModule.DEFAULT_REGISTRY
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
                        parameter("n", N)
                    }.body<TagsListResponse>().tags
                }.fold(
                    {
                        LoadResult.Page(it, null, repository to it.lastOrNull())
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