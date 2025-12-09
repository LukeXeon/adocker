package com.github.adocker.daemon.containers

import com.github.adocker.daemon.app.AppContext
import com.github.adocker.daemon.database.dao.ContainerDao
import com.github.adocker.daemon.database.dao.ImageDao
import com.github.adocker.daemon.database.model.ContainerEntity
import com.github.adocker.daemon.registry.model.ContainerConfig
import com.github.adocker.daemon.utils.deleteRecursively
import com.github.adocker.daemon.utils.extractTarGz
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified container manager that handles container lifecycle, execution, and state management.
 *
 * This class replaces ContainerExecutor and ContainerRepository, providing:
 * - Container CRUD operations (create, delete, rename)
 * - Container lifecycle management (start, stop, pause)
 * - Container state tracking (Created, Running, Exited, etc.)
 * - Thread-safe operations with coroutine synchronization
 *
 * All operations are thread-safe and use proper coroutine synchronization.
 */
@Singleton
class ContainerManager @Inject constructor(
    private val containerDao: ContainerDao,
    private val imageDao: ImageDao,
    private val appContext: AppContext,
    private val contextFactory: ContainerContext.Factory,
    private val containerFactory: RunningContainer.Factory,
    private val scope: CoroutineScope,
) {
    private val mutex = Mutex()

    /**
     * Maps container ID to its runtime state.
     * Only contains entries for containers that are currently running or being removed.
     */
    private val runtimeStates = MutableStateFlow<Map<String, RuntimeState>>(emptyMap())

    /**
     * Get all containers with their current state.
     *
     * @return Flow of containers with runtime state information
     */
    fun getAllContainers(): Flow<List<Container>> {
        return combine(
            containerDao.getAllContainers(),
            runtimeStates
        ) { entities, states ->
            entities.map { entity ->
                toContainer(entity, states[entity.id])
            }
        }
    }

    /**
     * Get a specific container by ID.
     */
    suspend fun getContainerById(id: String): Container? {
        val entity = containerDao.getContainerById(id) ?: return null
        val state = runtimeStates.value[id]
        return toContainer(entity, state)
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

            val entity = ContainerEntity(
                id = containerId,
                name = containerName,
                imageId = image.id,
                imageName = "${image.repository}:${image.tag}",
                config = mergedConfig
            )
            containerDao.insertContainer(entity)

            toContainer(entity, null)
        }
    }

    /**
     * Start a container.
     *
     * @param containerId The container ID to start
     * @return Result indicating success or failure
     */
    suspend fun startContainer(containerId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            mutex.withLock {
                val currentState = runtimeStates.value[containerId]

                // Check if already running
                if (currentState is RuntimeState.Running && currentState.job.isActive) {
                    throw IllegalStateException("Container is already running")
                }

                withContext(NonCancellable) {
                    // Start the container process
                    val context = contextFactory.create(containerId)
                    val process = context.startProcess().getOrThrow()
                    val runningContainer = containerFactory.create(context, process)

                    // Create runtime state
                    val runtimeState = RuntimeState.Running(
                        runningContainer = runningContainer,
                        job = runningContainer.job,
                        stdout = runningContainer.stdout,
                        stderr = runningContainer.stderr,
                        startedAt = System.currentTimeMillis()
                    )

                    // Update runtime states
                    runtimeStates.value = runtimeStates.value + (containerId to runtimeState)

                    // Mark container as having been run with current timestamp
                    containerDao.setContainerLastRun(containerId, System.currentTimeMillis())

                    // Monitor the job and update state when it completes
                    scope.launch {
                        try {
                            runningContainer.job.join()
                            val exitCode = withContext(Dispatchers.IO) {
                                runInterruptible {
                                    process.exitValue()
                                }
                            }
                            updateStateToExited(containerId, exitCode)
                        } catch (e: Exception) {
                            Timber.e(e, "Error monitoring container $containerId")
                            updateStateToExited(containerId, null)
                        }
                    }

                    // Return success
                    Unit
                }
            }
        }
    }

    /**
     * Stop a running container.
     *
     * @param containerId The container ID to stop
     * @return Result indicating success or failure
     */
    suspend fun stopContainer(containerId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            mutex.withLock {
                val state = runtimeStates.value[containerId]

                if (state !is RuntimeState.Running) {
                    throw IllegalStateException("Container is not running")
                }

                withContext(NonCancellable) {
                    // Cancel the job and wait for it to complete
                    state.job.cancelAndJoin()

                    // Get exit code if available
                    val exitCode = try {
                        state.runningContainer.mainProcess.exitValue()
                    } catch (e: IllegalThreadStateException) {
                        null // Process hasn't exited yet
                    }

                    // Update to exited state
                    updateStateToExited(containerId, exitCode)
                }
            }
        }
    }

    /**
     * Pause a container (not supported in PRoot).
     *
     * This operation is a no-op as PRoot doesn't support pausing containers.
     * We simply log a warning and return success.
     */
    suspend fun pauseContainer(containerId: String): Result<Unit> {
        Timber.w("Pause operation is not supported in PRoot-based containers (containerId: $containerId)")
        return Result.success(Unit)
    }

    /**
     * Unpause a container (not supported in PRoot).
     *
     * This operation is a no-op as PRoot doesn't support pausing containers.
     * We simply log a warning and return success.
     */
    suspend fun unpauseContainer(containerId: String): Result<Unit> {
        Timber.w("Unpause operation is not supported in PRoot-based containers (containerId: $containerId)")
        return Result.success(Unit)
    }

    /**
     * Delete a container.
     *
     * @param containerId The container ID to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteContainer(containerId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            mutex.withLock {
                val entity = containerDao.getContainerById(containerId)
                    ?: throw IllegalArgumentException("Container not found: $containerId")

                val state = runtimeStates.value[containerId]

                // Cannot delete a running container
                if (state is RuntimeState.Running && state.job.isActive) {
                    throw IllegalStateException("Cannot delete a running container. Stop it first.")
                }

                withContext(NonCancellable) {
                    try {
                        // Mark as removing
                        runtimeStates.value =
                            runtimeStates.value + (containerId to RuntimeState.Removing)

                        // Delete container directory
                        val containerDir = File(appContext.containersDir, containerId)
                        deleteRecursively(containerDir)

                        // Delete from database
                        containerDao.deleteContainerById(containerId)

                        // Remove from runtime states
                        runtimeStates.value = runtimeStates.value - containerId

                    } catch (e: Exception) {
                        // If cleanup fails, mark as Dead
                        Timber.e(e, "Failed to delete container $containerId")
                        runtimeStates.value =
                            runtimeStates.value + (containerId to RuntimeState.Dead(e.message))
                        throw e
                    }
                }
            }
        }
    }

    /**
     * Rename a container.
     *
     * @param containerId The container ID to rename
     * @param newName The new container name
     * @return Result indicating success or failure
     */
    suspend fun renameContainer(containerId: String, newName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                mutex.withLock {
                    val existing = containerDao.getContainerByName(newName)
                    if (existing != null && existing.id != containerId) {
                        throw IllegalArgumentException("Container with name '$newName' already exists")
                    }

                    val container = containerDao.getContainerById(containerId)
                        ?: throw IllegalArgumentException("Container not found: $containerId")

                    containerDao.updateContainer(container.copy(name = newName))
                }
            }
        }

    /**
     * Execute a command in a running container.
     *
     * @param containerId The container ID
     * @param command The command to execute
     * @return Result containing the process
     */
    suspend fun execCommand(containerId: String, command: List<String>): Result<Process> {
        return mutex.withLock {
            val state = runtimeStates.value[containerId]

            if (state !is RuntimeState.Running || !state.job.isActive) {
                return Result.failure(IllegalStateException("Container is not running"))
            }

            state.runningContainer.execCommand(command)
        }
    }

    /**
     * Get the running container instance for low-level operations.
     *
     * @param containerId The container ID
     * @return The running container if it exists and is active
     */
    suspend fun getRunningContainer(containerId: String): RunningContainer? {
        return mutex.withLock {
            val state = runtimeStates.value[containerId]
            if (state is RuntimeState.Running && state.job.isActive) {
                state.runningContainer
            } else {
                null
            }
        }
    }

    /**
     * Update container state to Exited.
     */
    private suspend fun updateStateToExited(containerId: String, exitCode: Int?) {
        mutex.withLock {
            runtimeStates.value =
                runtimeStates.value + (containerId to RuntimeState.Exited(exitCode))
        }
    }

    /**
     * Convert entity and runtime state to Container model.
     */
    private fun toContainer(entity: ContainerEntity, state: RuntimeState?): Container {
        val containerState = when (state) {
            null -> if (entity.lastRunAt != null) ContainerState.Exited else ContainerState.Created
            is RuntimeState.Running -> ContainerState.Running
            is RuntimeState.Exited -> ContainerState.Exited
            is RuntimeState.Removing -> ContainerState.Removing
            is RuntimeState.Dead -> ContainerState.Dead
        }

        return Container(
            id = entity.id,
            name = entity.name,
            imageId = entity.imageId,
            imageName = entity.imageName,
            createdAt = entity.createdAt,
            config = entity.config,
            state = containerState,
            lastRunAt = entity.lastRunAt,
            stdout = (state as? RuntimeState.Running)?.stdout,
            stderr = (state as? RuntimeState.Running)?.stderr,
            exitCode = (state as? RuntimeState.Exited)?.exitCode
        )
    }

    /**
     * Internal runtime state representation.
     */
    private sealed interface RuntimeState {
        data class Running(
            val runningContainer: RunningContainer,
            val job: Job,
            val stdout: File,
            val stderr: File,
            val startedAt: Long
        ) : RuntimeState

        data class Exited(val exitCode: Int?) : RuntimeState
        data object Removing : RuntimeState
        data class Dead(val error: String?) : RuntimeState
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
