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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
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
    private val stateMachineFactory: StateMachineFactory,
    private val scope: CoroutineScope,
) {
    private val mutex = Mutex()

    /**
     * Get all containers with their current state.
     *
     * @return Flow of containers with runtime state information
     */
    fun getAllContainers(): Flow<List<Container>> {
        return containerDao.getAllContainers()
            .scan(HashMap<String, Container>()) { cache, newList ->
                val keysToRemove = cache.keys - newList.asSequence().map { it.id }.toSet()
                keysToRemove.forEach { cache.remove(it) }
                newList.forEach { item ->
                    cache.getOrPut(item.id) {
                        Container(
                            item.id,
                            stateMachineFactory.launchIn(scope)
                        )
                    }
                }
                HashMap(cache)
            }.map {
                it.values.toList()
            }
    }

    /**
     * Get a specific container by ID.
     */
    suspend fun getContainerById(id: String): Container2? {
        val entity = containerDao.getContainerById(id) ?: return null
        val state = containers.value[id]
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
    ): Result<Container> {
        val stateMachine = stateMachineFactory.launchIn(scope)
        stateMachine.dispatch(
            ContainerOperation.Create(
                imageId,
                name,
                config,
            )
        )


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
                val currentState = containers.value[containerId]

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
                    containers.value = containers.value + (containerId to runtimeState)

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
                val state = containers.value[containerId]

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

                val state = containers.value[containerId]

                // Cannot delete a running container
                if (state is RuntimeState.Running && state.job.isActive) {
                    throw IllegalStateException("Cannot delete a running container. Stop it first.")
                }

                withContext(NonCancellable) {
                    try {
                        // Mark as removing
                        containers.value =
                            containers.value + (containerId to RuntimeState.Removing)

                        // Delete container directory
                        val containerDir = File(appContext.containersDir, containerId)
                        deleteRecursively(containerDir)

                        // Delete from database
                        containerDao.deleteContainerById(containerId)

                        // Remove from runtime states
                        containers.value = containers.value - containerId

                    } catch (e: Exception) {
                        // If cleanup fails, mark as Dead
                        Timber.e(e, "Failed to delete container $containerId")
                        containers.value =
                            containers.value + (containerId to RuntimeState.Dead(e.message))
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
            val state = containers.value[containerId]

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
            val state = containers.value[containerId]
            if (state is RuntimeState.Running && state.job.isActive) {
                state.runningContainer
            } else {
                null
            }
        }
    }


}
