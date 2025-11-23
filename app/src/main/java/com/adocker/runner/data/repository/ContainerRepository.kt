package com.adocker.runner.data.repository

import android.system.Os
import android.system.OsConstants
import com.adocker.runner.core.config.Config
import com.adocker.runner.core.utils.FileUtils
import com.adocker.runner.data.local.dao.ContainerDao
import com.adocker.runner.data.local.dao.ImageDao
import com.adocker.runner.data.local.entity.ContainerEntity
import com.adocker.runner.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Repository for container management - equivalent to udocker's ContainerStructure
 */
class ContainerRepository(
    private val containerDao: ContainerDao,
    private val imageDao: ImageDao
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    /**
     * Get all containers
     */
    fun getAllContainers(): Flow<List<Container>> {
        return containerDao.getAllContainers().map { entities ->
            entities.map { it.toContainer() }
        }
    }

    /**
     * Get container by ID
     */
    suspend fun getContainerById(id: String): Container? {
        return containerDao.getContainerById(id)?.toContainer()
    }

    /**
     * Get container by name
     */
    suspend fun getContainerByName(name: String): Container? {
        return containerDao.getContainerByName(name)?.toContainer()
    }

    /**
     * Create a new container from an image
     */
    suspend fun createContainer(
        imageId: String,
        name: String? = null,
        config: ContainerConfig = ContainerConfig()
    ): Result<Container> = withContext(Dispatchers.IO) {
        runCatching {
            val image = imageDao.getImageById(imageId)
                ?: throw IllegalArgumentException("Image not found: $imageId")

            // Generate container name if not provided
            val containerName = name ?: generateContainerName()

            // Check if name already exists
            if (containerDao.getContainerByName(containerName) != null) {
                throw IllegalArgumentException("Container with name '$containerName' already exists")
            }

            val containerId = java.util.UUID.randomUUID().toString().take(12)

            // Create container directory structure
            val containerDir = File(Config.containersDir, containerId)
            val rootfsDir = File(containerDir, Config.ROOTFS_DIR)
            rootfsDir.mkdirs()

            // Copy layers to rootfs (overlay simulation)
            image.layerIds.forEach { digest ->
                val layerDir = File(Config.layersDir, digest.removePrefix("sha256:"))
                if (layerDir.exists()) {
                    copyLayerToRootfs(layerDir, rootfsDir)
                }
            }

            // Merge image config with provided config
            val imageConfig = image.configJson?.let { json.decodeFromString<ImageConfig>(it) }
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

            val container = Container(
                id = containerId,
                name = containerName,
                imageId = image.id,
                imageName = "${image.repository}:${image.tag}",
                config = mergedConfig
            )

            // Save container metadata
            val containerJson = File(containerDir, Config.CONTAINER_JSON)
            containerJson.writeText(json.encodeToString(container))

            containerDao.insertContainer(container.toEntity())
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
                    android.util.Log.d("ContainerRepository", "✓ Copied symlink: ${destFile.name} -> $linkTarget")
                    return@forEach
                }
            } catch (e: Exception) {
                android.util.Log.w("ContainerRepository", "Failed to check if ${file.name} is symlink", e)
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
                        Os.chmod(destFile.absolutePath, stat.st_mode)
                    } catch (e: Exception) {
                        android.util.Log.w("ContainerRepository", "Failed to preserve permissions for ${file.name}", e)
                        // Fallback to Java File API
                        destFile.setReadable(file.canRead())
                        destFile.setWritable(file.canWrite())
                        destFile.setExecutable(file.canExecute())
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

            if (container.status == "RUNNING") {
                throw IllegalStateException("Cannot delete running container. Stop it first.")
            }

            // Delete container directory
            val containerDir = File(Config.containersDir, containerId)
            FileUtils.deleteRecursively(containerDir)

            containerDao.deleteContainerById(containerId)
        }
    }

    /**
     * Update container status
     */
    suspend fun updateContainerStatus(containerId: String, status: ContainerStatus) {
        containerDao.updateContainerStatus(containerId, status.name)
    }

    /**
     * Update container as running with PID
     */
    suspend fun setContainerRunning(containerId: String, pid: Int) {
        containerDao.updateContainerRunning(containerId, pid, ContainerStatus.RUNNING.name)
    }

    /**
     * Update container as stopped
     */
    suspend fun setContainerStopped(containerId: String) {
        containerDao.updateContainerRunning(containerId, null, ContainerStatus.STOPPED.name)
    }

    /**
     * Get container rootfs directory
     */
    fun getContainerRootfs(containerId: String): File {
        return File(Config.containersDir, "$containerId/${Config.ROOTFS_DIR}")
    }

    /**
     * Get container directory
     */
    fun getContainerDir(containerId: String): File {
        return File(Config.containersDir, containerId)
    }

    /**
     * Rename container
     */
    suspend fun renameContainer(containerId: String, newName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val existing = containerDao.getContainerByName(newName)
            if (existing != null && existing.id != containerId) {
                throw IllegalArgumentException("Container with name '$newName' already exists")
            }

            val container = containerDao.getContainerById(containerId)?.toContainer()
                ?: throw IllegalArgumentException("Container not found: $containerId")

            containerDao.updateContainer(container.copy(name = newName).toEntity())
        }
    }

    /**
     * Generate a random container name
     */
    private fun generateContainerName(): String {
        val adjectives = listOf("happy", "sleepy", "brave", "clever", "swift", "calm", "eager", "fancy")
        val nouns = listOf("panda", "tiger", "eagle", "dolphin", "falcon", "wolf", "bear", "lion")
        val adj = adjectives.random()
        val noun = nouns.random()
        val num = (1000..9999).random()
        return "${adj}_${noun}_$num"
    }

    private fun ContainerEntity.toContainer(): Container {
        return Container(
            id = id,
            name = name,
            imageId = imageId,
            imageName = imageName,
            created = created,
            status = ContainerStatus.valueOf(status),
            config = json.decodeFromString(configJson),
            pid = pid
        )
    }

    private fun Container.toEntity(): ContainerEntity {
        return ContainerEntity(
            id = id,
            name = name,
            imageId = imageId,
            imageName = imageName,
            created = created,
            status = status.name,
            configJson = json.encodeToString(config),
            pid = pid
        )
    }
}
