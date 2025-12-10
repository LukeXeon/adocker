package com.github.adocker.daemon.containers

import android.app.Application
import com.github.adocker.daemon.R
import com.github.adocker.daemon.app.AppContext
import com.github.adocker.daemon.database.dao.ContainerDao
import com.github.adocker.daemon.database.dao.ImageDao
import com.github.adocker.daemon.database.model.ContainerEntity
import com.github.adocker.daemon.registry.model.ContainerConfig
import com.github.adocker.daemon.utils.extractTarGz
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
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
    private val factory: StateMachineFactory.Factory,
    private val containerDao: ContainerDao,
    private val imageDao: ImageDao,
    private val appContext: AppContext,
    private val scope: CoroutineScope,
    application: Application,
) {
    private val adjectives = application.resources.getStringArray(R.array.adjectives).asList()
    private val nouns = application.resources.getStringArray(R.array.nouns).asList()

    // Cache for Container instances (protected by mutex for thread safety)
    private val containers = mutableMapOf<String, Container>()
    // Mutex to ensure atomic updates to the cache
    private val containersMutex = Mutex()

    /**
     * Generate a random container name
     */
    private fun generateContainerName(): String {
        val adj = adjectives.random()
        val noun = nouns.random()
        val num = (1000..9999).random()
        return "${adj}_${noun}_$num"
    }

    private suspend fun generateContainerSafeName(): String {
        var name = generateContainerName()
        val context = currentCoroutineContext()
        while (context.isActive) {
            if (containerDao.getContainerByName(name) != null) {
                name = generateContainerName()
            } else {
                break
            }
        }
        return name
    }

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
                    Container(
                        containerId,
                        factory.create(ContainerState.Exited(containerId)).launchIn(scope)
                    )
                )
            }

            else -> {
                Result.success(
                    Container(
                        containerId,
                        factory.create(ContainerState.Created(containerId)).launchIn(scope)
                    )
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
            generateContainerSafeName()
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
                }.fold({
                    Timber.d("Layer ${digest.take(16)} extracted successfully")
                }, {
                    return Result.failure(
                        it
                    )
                })
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
            Container(
                containerId, factory.create(ContainerState.Created(containerId)).launchIn(scope)
            )
        )
    }

    /**
     * Get all containers as a Flow that reuses Container instances.
     * Only creates/removes Container instances when the ID set changes.
     * Thread-safe implementation using Mutex.
     */
    fun getAllContainers(): Flow<List<Container>> {
        return containerDao.getAllContainers().map { containerIds ->
            // Use mutex to ensure atomic cache updates
            containersMutex.withLock {
                // Get current cached IDs
                val cachedIds = containers.keys.toSet()
                val newIds = containerIds.toSet()

                // Remove containers that no longer exist
                val idsToRemove = cachedIds - newIds
                idsToRemove.forEach { id ->
                    containers.remove(id)
                    Timber.d("Removed container from cache: $id")
                }

                // Add new containers
                val idsToAdd = newIds - cachedIds
                for (id in idsToAdd) {
                    val result = loadContainer(id)
                    result.fold(
                        onSuccess = { container ->
                            containers[id] = container
                            Timber.d("Added container to cache: $id")
                        },
                        onFailure = { error ->
                            Timber.e(error, "Failed to load container: $id")
                        }
                    )
                }

                // Return containers in the order from database
                containerIds.mapNotNull { id ->
                    containers[id] // Returns null if load failed
                }
            }
        }
    }
}

