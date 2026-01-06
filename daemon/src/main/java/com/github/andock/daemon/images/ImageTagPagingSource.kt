package com.github.andock.daemon.images

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.andock.daemon.database.dao.AuthTokenDao
import com.github.andock.daemon.images.model.TagsListResponse
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Singleton

class ImageTagPagingSource @AssistedInject constructor(
    private val repositories: ImageRepositories,
    private val client: HttpClient,
    private val authTokenDao: AuthTokenDao,
) : PagingSource<ImageTagKey, String>() {

    override fun getRefreshKey(
        state: PagingState<ImageTagKey, String>
    ): ImageTagKey? = null

    override suspend fun load(params: LoadParams<ImageTagKey>): LoadResult<ImageTagKey, String> {
        val key = params.key ?: return LoadResult.Page(emptyList(), null, null)
        val (registryUrl, repository, pageSize, last) = key
        val imageRepository = repositories[registryUrl]
        return withContext(Dispatchers.IO) {
            imageRepository.authenticate(repository)
                .mapCatching { authToken ->
                    val response = client.get("$registryUrl/v2/${repository}/tags/list") {
                        if (authToken.isNotEmpty()) {
                            header(HttpHeaders.Authorization, "Bearer $authToken")
                        }
                        if (!last.isNullOrEmpty()) {
                            parameter("last", last)
                        }
                        parameter("n", pageSize.toString())
                    }
                    if (response.status == HttpStatusCode.Unauthorized) {
                        authTokenDao.deleteToken(authToken)
                    }
                    response.body<TagsListResponse>().tags
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
                                nextKey = key.copy(last = if (it.size == pageSize) it.lastOrNull() else null)
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