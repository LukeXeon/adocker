package com.github.andock.daemon.containers

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.andock.daemon.database.dao.LogLineDao
import com.github.andock.daemon.database.model.LogLineDTO
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Singleton

class ContainerLogPagingSource @AssistedInject constructor(
    private val logLineDao: LogLineDao,
) : PagingSource<ContainerLogKey, LogLineDTO>() {

    override fun getRefreshKey(
        state: PagingState<ContainerLogKey, LogLineDTO>
    ): ContainerLogKey? = null

    override suspend fun load(params: LoadParams<ContainerLogKey>): LoadResult<ContainerLogKey, LogLineDTO> {
        val key = params.key
        val (containerId, currentPage, pageSize) = key ?: return LoadResult.Page(
            emptyList(),
            null,
            null
        )
        try {
            val offset = (currentPage - 1) * pageSize
            // 从Room查询数据
            val data = logLineDao.getLogLines(containerId, offset, pageSize)
            // 计算总页数(可选)
            val totalCount = logLineDao.getTotalCount(containerId)
            val totalPages = (totalCount + pageSize - 1) / pageSize
            // 构建分页结果
            return LoadResult.Page(
                data = data,
                prevKey = if (currentPage > 1) key.copy(currentPage = currentPage - 1) else null,
                nextKey = if (currentPage < totalPages) key.copy(currentPage = currentPage + 1) else null
            )
        } catch (e: Throwable) {
            return LoadResult.Error(e)
        }
    }

    @Singleton
    @AssistedFactory
    interface Factory : () -> ContainerLogPagingSource {
        override fun invoke(): ContainerLogPagingSource
    }
}