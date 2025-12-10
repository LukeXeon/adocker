package com.github.adocker.daemon.containers

import com.github.adocker.daemon.database.dao.ContainerDao
import com.github.adocker.daemon.registry.model.ContainerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ContainerManager @Inject constructor(
    private val containerDao: ContainerDao,
    private val containerFactory: ContainerFactory
) {
    fun getAllContainers(): Flow<List<Container>> {
        return containerDao.getAllContainers()
            .scan(HashMap<String, Container>()) { cache, newList ->
                val toRemove = cache.keys - newList.asSequence().map { it.id }.toSet()
                toRemove.forEach { cache.remove(it) }
                newList.forEach { item ->
                    cache.getOrPut(item.id) {
                        containerFactory.loadContainer(item.id).getOrThrow()
                    }
                }
                HashMap(cache)
            }.map {
                it.values.toList()
            }
    }

    /**
     * Create a new container from an image.
     *
     * @param imageId The image ID to create the container from
     * @param name Optional container name (auto-generated if null)
     * @param config Container configuration (command, env, etc.)
     * @return Result containing the created container
     */
    suspend fun createContainer(
        imageId: String,
        name: String? = null,
        config: ContainerConfig = ContainerConfig()
    ): Result<Container> {
        return containerFactory.createContainer(imageId, name, config)
    }


}
