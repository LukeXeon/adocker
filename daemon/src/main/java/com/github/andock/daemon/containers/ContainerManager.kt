package com.github.andock.daemon.containers

import com.github.andock.daemon.containers.creator.ContainerCreator
import com.github.andock.daemon.database.dao.ContainerDao
import com.github.andock.daemon.images.models.ContainerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ContainerManager @Inject constructor(
    private val containerFactory: Container.Factory,
    private val containerDao: ContainerDao,
    private val creatorFactory: ContainerCreator.Factory,
    scope: CoroutineScope,
) {
    private val _containers = MutableStateFlow<Map<String, Container>>(emptyMap())

    val containers = _containers.asStateFlow()
    val sortedList = _containers.map {
        it.asSequence().sortedBy { container -> container.key }
            .map { container -> container.value }
            .toList()
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun filterState(predicate: (ContainerState) -> Boolean): Flow<List<Container>> {
        return _containers.flatMapLatest { containers ->
            if (containers.isEmpty()) {
                flowOf(emptyList())
            } else {
                // Combine all metadata flows
                combine(
                    containers.asSequence()
                        .sortedBy { it.key }
                        .map {
                            it.value
                        }.map { it.state }
                        .asIterable()
                ) { metadataArray ->
                    // Pair each metadata with its registry and sort by metadata
                    metadataArray.asSequence()
                        .filter(predicate)
                        .mapNotNull { metadata ->
                            val container = containers[metadata.id]
                            if (container != null) {
                                return@mapNotNull container
                            } else {
                                return@mapNotNull null
                            }
                        }.sortedBy { it.id }
                        .toList()
                }
            }
        }
    }

    init {
        scope.launch {
            _containers.value = containerDao.getAllLastRun().map { entity ->
                containerFactory.create(
                    if (entity.lastRunAt != null) {
                        ContainerState.Exited(entity.id)
                    } else {
                        ContainerState.Created(entity.id)
                    }
                )
            }.associateBy { it.id }
        }
    }

    internal fun removeContainer(containerId: String) {
        _containers.update {
            it - containerId
        }
    }

    internal fun addContainer(container: Container) {
        _containers.update {
            it + (container.id to container)
        }
    }

    fun createContainer(
        imageId: String,
        name: String?,
        config: ContainerConfig,
    ): ContainerCreator {
        return creatorFactory.create(imageId, name, config)
    }
}

