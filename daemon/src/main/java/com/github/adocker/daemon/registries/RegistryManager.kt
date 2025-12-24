package com.github.adocker.daemon.registries

import com.github.adocker.daemon.app.AppContext
import com.github.adocker.daemon.database.dao.RegistryDao
import com.github.adocker.daemon.database.model.RegistryEntity
import com.github.adocker.daemon.database.model.RegistryType
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
    scope: CoroutineScope
) {
    companion object {
        // Builtin servers for Docker Hub
        private val BUILTIN_SERVERS = listOf(
            RegistryEntity(
                id = "c0d1e2f3-4567-49ab-cdef-0123456789ab",
                name = "Docker Hub",
                url = AppContext.DEFAULT_REGISTRY,
                priority = 100,
                type = RegistryType.Official,
            ),
            RegistryEntity(
                id = "3f8e7d6c-5b4a-4876-80fe-dcba98765432",
                name = "DaoCloud (China)",
                url = "https://docker.m.daocloud.io",
                priority = 100,
                type = RegistryType.BuiltinMirror,
            ),
            RegistryEntity(
                id = "789abcde-1234-4678-90ab-cdef12345678",
                name = "Xuanyuan (China)",
                url = "https://docker.xuanyuan.me",
                priority = 90,
                type = RegistryType.BuiltinMirror,
            ),
            RegistryEntity(
                id = "87654321-abcd-4f12-3456-7890abcdef12",
                name = "Huawei Cloud (China)",
                url = "https://mirrors.huaweicloud.com",
                priority = 80,
                type = RegistryType.BuiltinMirror,
            )
        )
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
                    .map { it.value.metadata }
                    .asIterable()
            ) { metadataArray ->
                // Pair each metadata with its registry and sort by metadata
                metadataArray.asSequence()
                    .filterNotNull()
                    .mapNotNull { metadata ->
                        registries[metadata.id]?.let { registry ->
                            registry to metadata
                        }
                    }.sortedBy { it.second }
                    .map { it.first }
                    .toList()
            }
        }
    }.stateIn(scope, SharingStarted.Lazily, emptyList())

    init {
        scope.launch {
            registryDao.insertRegistries(BUILTIN_SERVERS)
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
}