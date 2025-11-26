package com.github.adocker.core.container

import android.system.Os
import android.system.OsConstants
import com.github.adocker.core.config.AppConfig
import com.github.adocker.core.database.AppDatabase
import com.github.adocker.core.database.dao.ContainerDao
import com.github.adocker.core.database.dao.ImageDao
import com.github.adocker.core.registry.model.ContainerConfig
import com.github.adocker.core.database.model.ContainerEntity
import com.github.adocker.core.database.model.ContainerStatus
import com.github.adocker.core.utils.chmod
import com.github.adocker.core.utils.deleteRecursively
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for container management - equivalent to udocker's ContainerStructure
 */
@Singleton
class ContainerRepository @Inject constructor(
    private val containerDao: ContainerDao,
    private val imageDao: ImageDao,
    private val appConfig: AppConfig,
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
            val containerDir = File(appConfig.containersDir, containerId)
            val rootfsDir = File(containerDir, AppConfig.Companion.ROOTFS_DIR)
            rootfsDir.mkdirs()

            // Copy layers to rootfs (overlay simulation)
            image.layerIds.forEach { digest ->
                val layerDir = File(appConfig.layersDir, digest.removePrefix("sha256:"))
                if (layerDir.exists()) {
                    copyLayerToRootfs(layerDir, rootfsDir)
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

            // Save container to database
            containerDao.insertContainer(container)
            container
        }
    }

    /**
     * Copy layer contents to rootfs
     */
    private fun copyLayerToRootfs(layerDir: File, rootfsDir: File) {
        layerDir.walkTopDown().forEach { file ->
            if (file == layerDir) return@forEach

            val relativePath = file.relativeTo(layerDir).path
            val destFile = File(rootfsDir, relativePath)

            // Handle whiteout files (Docker layer deletion markers)
            if (file.name.startsWith(".wh.")) {
                val targetName = file.name.removePrefix(".wh.")
                val targetFile = File(destFile.parentFile, targetName)
                targetFile.deleteRecursively()
                return@forEach
            }

            // Handle opaque whiteout (replace entire directory)
            if (file.name == ".wh..wh..opq") {
                destFile.parentFile?.listFiles()?.forEach { it.deleteRecursively() }
                return@forEach
            }
            // Check if it's a symbolic link using Os.lstat
            try {
                val stat = Os.lstat(file.absolutePath)
                if (OsConstants.S_ISLNK(stat.st_mode)) {
                    // It's a symbolic link - read the target and recreate it
                    val linkTarget = Os.readlink(file.absolutePath)
                    destFile.parentFile?.mkdirs()

                    // Delete existing file/link if it exists
                    if (destFile.exists()) {
                        destFile.delete()
                    }

                    // Create the symlink at destination
                    Os.symlink(linkTarget, destFile.absolutePath)
                    Timber.Forest.d("✓ Copied symlink: ${destFile.name} -> $linkTarget")
                    return@forEach
                }
            } catch (e: Exception) {
                Timber.Forest.w(e, "Failed to check if ${file.name} is symlink")
                // Fall through to regular file handling
            }

            when {
                file.isDirectory -> destFile.mkdirs()
                file.isFile -> {
                    destFile.parentFile?.mkdirs()
                    file.copyTo(destFile, overwrite = true)
                    // Preserve permissions using Os.chmod
                    try {
                        val stat = Os.lstat(file.absolutePath)
                        destFile.chmod(stat.st_mode)
                    } catch (e: Exception) {
                        Timber.Forest.w(e, "Failed to preserve permissions for ${file.name}")
                    }
                }
            }
        }
    }

    /**
     * Delete a container
     */
    suspend fun deleteContainer(containerId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val container = containerDao.getContainerById(containerId)
                ?: throw IllegalArgumentException("Container not found: $containerId")

            // Note: Caller should ensure container is stopped before deletion

            // Delete container directory
            val containerDir = File(appConfig.containersDir, containerId)
            deleteRecursively(containerDir)

            containerDao.deleteContainerById(containerId)
        }
    }


    /**
     * Get container rootfs directory
     */
    fun getContainerRootfs(containerId: String): File {
        return File(appConfig.containersDir, "$containerId/${AppConfig.Companion.ROOTFS_DIR}")
    }

    /**
     * Get container directory
     */
    fun getContainerDir(containerId: String): File {
        return File(appConfig.containersDir, containerId)
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