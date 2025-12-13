package com.github.adocker.daemon.containers

import com.github.adocker.daemon.app.AppContext
import com.github.adocker.daemon.database.dao.ContainerDao
import com.github.adocker.daemon.database.dao.ImageDao
import com.github.adocker.daemon.database.model.ContainerEntity
import com.github.adocker.daemon.registry.model.ContainerConfig
import com.github.adocker.daemon.io.extractTarGz
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContainerManager @Inject constructor(
    private val factory: Container.Factory,
    private val containerDao: ContainerDao,
    private val imageDao: ImageDao,
    private val appContext: AppContext,
    private val containerName: ContainerName,
    scope: CoroutineScope,
) {
    private suspend fun loadContainer(containerId: String): Result<Container> {
        val entity = containerDao.getContainerById(containerId)
        return when {
            entity == null -> {
                Result.failure(
                    IllegalArgumentException("Container not found: $containerId")
                )
            }

            entity.lastRunAt != null -> {
                Result.success(
                    factory.create(ContainerState.Exited(containerId))
                )
            }

            else -> {
                Result.success(
                    factory.create(ContainerState.Created(containerId))
                )
            }
        }
    }

    suspend fun createContainer(
        imageId: String,
        name: String?,
        config: ContainerConfig = ContainerConfig(),
    ): Result<Container> {
        val image = imageDao.getImageById(imageId) ?: return Result.failure(
            IllegalArgumentException("Image not found: $imageId")
        )
        val containerName = if (name == null) {
            containerName.generateName()
        } else {
            if (containerDao.getContainerById(name) != null) {
                return Result.failure(
                    IllegalArgumentException("Container with name '${name}' already exists")
                )
            } else {
                name
            }
        }

        val containerId = UUID.randomUUID().toString()
        // Create container directory structure
        val containerDir = File(appContext.containersDir, containerId)
        val rootfsDir = File(containerDir, AppContext.ROOTFS_DIR)
        rootfsDir.mkdirs()
        // Extract layers directly to rootfs
        for (digest in image.layerIds) {
            val layerFile = File(
                appContext.layersDir, "${digest.removePrefix("sha256:")}.tar.gz"
            )
            if (layerFile.exists()) {
                Timber.d("Extracting layer ${digest.take(16)} to container rootfs")
                FileInputStream(layerFile).use { fis ->
                    extractTarGz(fis, rootfsDir)
                }.fold(
                    {
                        Timber.d("Layer ${digest.take(16)} extracted successfully")
                    },
                    {
                        return Result.failure(
                            it
                        )
                    }
                )
            } else {
                Timber.w("Layer file not found: ${layerFile.absolutePath}")
            }
        }
        // Merge image config with provided config
        val imageConfig = image.config
        val mergedConfig = config.copy(
            cmd = if (config.cmd == listOf("/bin/sh")) {
                imageConfig?.cmd ?: imageConfig?.entrypoint ?: config.cmd
            } else config.cmd,
            entrypoint = config.entrypoint ?: imageConfig?.entrypoint,
            env = buildMap {
                imageConfig?.env?.forEach { envStr ->
                    val parts = envStr.split("=", limit = 2)
                    if (parts.size == 2) {
                        put(parts[0], parts[1])
                    }
                }
                putAll(config.env)
            },
            workingDir = if (config.workingDir == "/") {
                imageConfig?.workingDir ?: config.workingDir
            } else {
                config.workingDir
            },
            user = if (config.user == "root") {
                imageConfig?.user ?: config.user
            } else {
                config.user
            }
        )
        val entity = ContainerEntity(
            id = containerId,
            name = containerName,
            imageId = image.id,
            imageName = "${image.repository}:${image.tag}",
            config = mergedConfig
        )
        containerDao.insertContainer(entity)
        return Result.success(
            factory.create(ContainerState.Created(containerId))
        )
    }

    val allContainers = flow {
        val table = HashMap<String, Container>()
        val mutex = Mutex()
        containerDao.getAllContainers().collectLatest { containerIds ->
            // Use mutex to ensure atomic cache updates
            val newList = mutex.withLock {
                // Get current cached IDs
                val oldIds = table.keys
                val newIds = containerIds.toSet()
                // Remove containers that no longer exist
                val idsToRemove = oldIds - newIds
                idsToRemove.forEach { id ->
                    table.remove(id)
                    Timber.d("Removed container from cache: $id")
                }
                // Add new containers
                val idsToAdd = newIds - oldIds
                idsToAdd.forEach { id ->
                    val result = loadContainer(id)
                    result.fold(
                        { container ->
                            table[id] = container
                            Timber.d("Added container to cache: $id")
                        },
                        { error ->
                            Timber.e(error, "Failed to load container: $id")
                        }
                    )
                }
                table.asSequence().sortedBy { it.key }.map { it.value }.toList()
            }
            emit(newList)
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

}

