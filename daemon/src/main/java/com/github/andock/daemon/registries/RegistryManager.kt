package com.github.andock.daemon.registries

import com.github.andock.daemon.database.dao.RegistryDao
import com.github.andock.daemon.database.model.RegistryEntity
import com.github.andock.daemon.database.model.RegistryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class RegistryManager @Inject constructor(
    private val registryDao: RegistryDao,
    private val factory: Registry.Factory,
    private val builtinServers: List<RegistryEntity>,
    scope: CoroutineScope
) {
    companion object {
        const val DEFAULT_REGISTRY = "https://registry-1.docker.io"
    }

    private val _registries = MutableStateFlow<Map<String, Registry>>(emptyMap())

    val registries = _registries.asStateFlow()

    val sortedList = _registries.flatMapLatest { registries ->
        if (registries.isEmpty()) {
            flowOf(emptyList())
        } else {
            // Combine all metadata flows
            combine(
                registries.asSequence()
                    .sortedBy { it.key }
                    .map {
                        it.value
                    }.map { it.metadata }
                    .asIterable()
            ) { metadataArray ->
                // Pair each metadata with its registry and sort by metadata
                metadataArray.asSequence()
                    .filterNotNull()
                    .mapNotNull { metadata ->
                        val registry = registries[metadata.id]
                        if (registry != null) {
                            return@mapNotNull registry to metadata
                        } else {
                            return@mapNotNull null
                        }
                    }.sortedBy { it.second }
                    .map { it.first }
                    .toList()
            }
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    val bestServer = _registries.flatMapLatest { registries ->
        if (registries.isEmpty()) {
            flowOf(null)
        } else {
            data class Snapshot(
                val registry: Registry,
                val state: RegistryState.Healthy,
                val metadata: RegistryEntity,
            ) : Comparable<Snapshot> {
                override fun compareTo(other: Snapshot): Int {
                    var c = this.state.compareTo(other.state)
                    if (c != 0) {
                        return c
                    }
                    c = this.metadata.compareTo(other.metadata)
                    if (c != 0) {
                        return c
                    }
                    return 0
                }
            }

            combine(
                registries.asSequence()
                    .sortedBy { it.key }
                    .map { it.value }
                    .map {
                        combine(it.state, it.metadata) { m, d ->
                            m to d
                        }
                    }.asIterable()
            ) { array ->
                array.asSequence().mapNotNull { (state, metadata) ->
                    if (metadata != null && state is RegistryState.Healthy) {
                        val registry = registries[metadata.id]
                        if (registry != null) {
                            return@mapNotNull Snapshot(
                                registry,
                                state,
                                metadata,
                            )
                        }
                    }
                    return@mapNotNull null
                }.sorted().map {
                    it.registry
                }.firstOrNull()
            }
        }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    init {
        scope.launch {
            registryDao.insertRegistries(builtinServers)
            _registries.value = registryDao.getAllRegistryIds().map { id ->
                factory.create(id)
            }.associateBy { it.id }
            while (isActive) {
                delay(with(Duration) {
                    5.minutes
                })
                checkAll()
            }
        }
    }

    suspend fun addCustomMirror(
        name: String,
        url: String,
        bearerToken: String? = null,
        priority: Int = 50
    ): Result<Registry> {
        val id = UUID.randomUUID().toString()
        val newMirror = RegistryEntity(
            id = id,
            url = url.removeSuffix("/"),
            name = name,
            bearerToken = bearerToken,
            priority = priority,
            type = RegistryType.CustomMirror
        )
        registryDao.insertRegistry(newMirror)
        val mirror = factory.create(id)
        _registries.update {
            it + (id to mirror)
        }
        return Result.success(mirror)
    }

    /**
     * Check health of all mirrors concurrently
     */
    suspend fun checkAll() {
        _registries.value.values.forEach {
            it.check()
        }
    }

    internal suspend fun removeServer(id: String) {
        registryDao.deleteRegistryById(id)
        _registries.update {
            it - id
        }
    }

    /**
     * Get registry URL for pulling images
     * For Docker Hub, automatically selects best mirror
     * For other registries, returns the original URL
     */
    fun getBestServerUrl(originalRegistry: String): String {
        return when {
            // Docker Hub - use best available mirror
            originalRegistry == "registry-1.docker.io"
                    || originalRegistry.contains("docker.io") -> {
                bestServer.value?.metadata?.value?.url
                    ?: RegistryModule.DEFAULT_REGISTRY
            }
            // Other registries - use as-is
            originalRegistry.startsWith("http") -> {
                originalRegistry
            }

            else -> {
                "https://$originalRegistry"
            }
        }
    }
}