package com.github.adocker.daemon.containers

import com.github.adocker.daemon.app.AppContext
import com.github.adocker.daemon.database.dao.ContainerDao
import com.github.adocker.daemon.database.dao.ImageDao
import com.github.adocker.daemon.database.model.ContainerEntity
import com.github.adocker.daemon.registry.model.ContainerConfig
import com.github.adocker.daemon.utils.deleteRecursively
import com.github.adocker.daemon.utils.extractTarGz
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for container management
 */
@Singleton
class ContainerRepository @Inject constructor(
    private val containerDao: ContainerDao,
    private val imageDao: ImageDao,
    private val appContext: AppContext,
) {

    /**
     * Get all containers
     */
    fun getAllContainers(): Flow<List<ContainerEntity>> {
        return containerDao.getAllContainers()
    }

    /**
     * Get container by ID
     */
    suspend fun getContainerById(id: String): ContainerEntity? {
        return containerDao.getContainerById(id)
    }

    /**
     * Get container by name
     */
    suspend fun getContainerByName(name: String): ContainerEntity? {
        return containerDao.getContainerByName(name)
    }

    /**
     * Create a new container from an image
     */
    suspend fun createContainer(
        imageId: String,
        name: String? = null,
        config: ContainerConfig = ContainerConfig()
    ): Result<ContainerEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val image = imageDao.getImageById(imageId)
                ?: throw IllegalArgumentException("Image not found: $imageId")

            // Generate container name if not provided
            val containerName = name ?: generateContainerName()

            // Check if name already exists
            if (containerDao.getContainerByName(containerName) != null) {
                throw IllegalArgumentException("Container with name '$containerName' already exists")
            }

            val containerId = UUID.randomUUID().toString()

            // Create container directory structure
            val containerDir = File(appContext.containersDir, containerId)
            val rootfsDir = File(containerDir, AppContext.Companion.ROOTFS_DIR)
            rootfsDir.mkdirs()

            // Extract layers directly to rootfs
            image.layerIds.forEach { digest ->
                val layerFile =
                    File(appContext.layersDir, "${digest.removePrefix("sha256:")}.tar.gz")
                if (layerFile.exists()) {
                    Timber.d("Extracting layer ${digest.take(16)} to container rootfs")
                    FileInputStream(layerFile).use { fis ->
                        extractTarGz(fis, rootfsDir).getOrThrow()
                    }
                    Timber.d("Layer ${digest.take(16)} extracted successfully")
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
                } else config.workingDir,
                user = if (config.user == "root") {
                    imageConfig?.user ?: config.user
                } else config.user
            )

            val container = ContainerEntity(
                id = containerId,
                name = containerName,
                imageId = image.id,
                imageName = "${image.repository}:${image.tag}",
                config = mergedConfig
            )
            containerDao.insertContainer(container)
            container
        }
    }


    /**
     * Delete a container
     */
    suspend fun deleteContainer(containerId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            containerDao.getContainerById(containerId)
                ?: throw IllegalArgumentException("Container not found: $containerId")
            // Delete container directory
            val containerDir = File(appContext.containersDir, containerId)
            deleteRecursively(containerDir)
            containerDao.deleteContainerById(containerId)
        }
    }


    /**
     * Rename container
     */
    suspend fun renameContainer(containerId: String, newName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val existing = containerDao.getContainerByName(newName)
                if (existing != null && existing.id != containerId) {
                    throw IllegalArgumentException("Container with name '$newName' already exists")
                }

                val container = containerDao.getContainerById(containerId)
                    ?: throw IllegalArgumentException("Container not found: $containerId")

                containerDao.updateContainer(container.copy(name = newName))
            }
        }

    companion object {
        /**
         * Generate a random container name
         */
        private fun generateContainerName(): String {
            val adj = ADJECTIVES.random()
            val noun = NOUNS.random()
            val num = (1000..9999).random()
            return "${adj}_${noun}_$num"
        }

        private val NOUNS = listOf(
            "panda", "tiger", "eagle", "dolphin", "falcon", "wolf", "bear", "lion"
        )

        private val ADJECTIVES = listOf(
            "happy", "sleepy", "brave", "clever", "swift", "calm", "eager", "fancy"
        )
    }

}