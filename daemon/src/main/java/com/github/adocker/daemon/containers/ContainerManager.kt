package com.github.adocker.daemon.containers

import com.github.adocker.daemon.app.AppContext
import com.github.adocker.daemon.client.model.ContainerConfig
import com.github.adocker.daemon.database.dao.ContainerDao
import com.github.adocker.daemon.database.dao.ImageDao
import com.github.adocker.daemon.database.model.ContainerEntity
import com.github.adocker.daemon.io.extractTarGz
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    private val _containers = MutableStateFlow<Map<String, Container>>(emptyMap())

    val containers = _containers.asStateFlow()

    init {
        scope.launch {
            _containers.value = containerDao.getAllContainers().map { entity ->
                factory.create(
                    if (entity.lastRunAt != null) {
                        ContainerState.Exited(entity.id)
                    } else {
                        ContainerState.Created(entity.id)
                    }
                )
            }.associateBy { it.id }
        }
    }

    internal suspend fun removeContainer(containerId: String) {
        containerDao.deleteContainerById(containerId)
        _containers.update {
            it - containerId
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
            if (containerDao.getContainerByName(name) != null) {
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
        val container = factory.create(ContainerState.Created(containerId))
        _containers.update {
            it + (containerId to container)
        }
        return Result.success(container)
    }
}

