package com.github.andock.daemon.images

import androidx.collection.lruCache
import com.github.andock.daemon.database.dao.RegistryDao
import com.github.andock.daemon.database.model.RegistryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageRepositories @Inject constructor(
    scope: CoroutineScope,
    registryDao: RegistryDao,
    builtinServers: List<RegistryEntity>,
    factory: ImageRepository.Factory,
) {
    private val cache = lruCache(builtinServers.size, create = factory)

    init {
        scope.launch {
            registryDao.getCountAsFlow().collect {
                cache.resize(maxOf(1, builtinServers.size, it))
            }
        }
    }

    operator fun get(key: String): ImageRepository {
        return cache[key]!!
    }
}