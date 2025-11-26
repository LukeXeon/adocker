package com.github.adocker.daemon.containers

import com.github.adocker.daemon.database.model.ContainerStatus
import com.github.adocker.daemon.engine.ExecResult
import com.github.adocker.daemon.engine.PRootEngine
import com.github.adocker.daemon.utils.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.DurationUnit

/**
 * Container executor - manages container lifecycle and execution
 * Equivalent to udocker's ExecutionEngine
 */
@Singleton
class ContainerExecutor @Inject constructor(
    private val prootEngine: PRootEngine,
    private val containerRepository: ContainerRepository,
    private val scope: CoroutineScope,
    private val factory: RunningProcess.Factory,
) {
    private val mutex = Mutex()
    private val runningContainers = MutableStateFlow<Map<String, List<RunningProcess>>>(emptyMap())

    /**
     * Run a container (start and attach)
     */
    suspend fun runContainer(
        containerId: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val container = containerRepository.getContainerById(containerId)
                ?: throw IllegalArgumentException("Container not found: $containerId")

            if (container.status == ContainerStatus.RUNNING) {
                throw IllegalStateException("Container is already running")
            }
            val process = prootEngine.startContainer(
                container = container,
                rootfsDir = rootfsDir,
            ).getOrThrow()
            // Update container status - use hashCode as fallback for process identification
            containerRepository.setContainerRunning(containerId, process.hashCode())
        }
    }

    /**
     * Start a container in the background
     */
    suspend fun startContainer(containerId: String): Result<Unit> {
        return runContainer(containerId)
    }

    /**
     * Stop a running container
     */
    suspend fun stopContainer(containerId: String, force: Boolean = false): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val handle = runningProcesses[containerId]
                    ?: throw IllegalStateException("Container is not running")

                if (force) {
                    handle.process.destroyForciblyCompat()
                } else {
                    handle.process.destroy()
                    // Give it some time to gracefully shutdown
                    val exited = handle.process.await(10, DurationUnit.SECONDS)
                    if (!exited) {
                        handle.process.destroyForciblyCompat()
                    }
                }

                handle.job?.cancel()
                runningProcesses.remove(containerId)
                containerRepository.setContainerStopped(containerId)
            }
        }

    /**
     * Execute a command in a running container
     */
    suspend fun execInContainer(
        containerId: String,
        command: List<String>,
        timeout: Long = 60000
    ): Result<ExecResult> = withContext(Dispatchers.IO) {
        runCatching {
            val container = containerRepository.getContainerById(containerId)
                ?: throw IllegalArgumentException("Container not found: $containerId")

            val rootfsDir = containerRepository.getContainerRootfs(containerId)

            prootEngine.execInContainer(
                container = container,
                rootfsDir = rootfsDir,
                command = command,
                timeout = timeout
            ).getOrThrow()
        }
    }

    /**
     * Execute a command and stream output
     */
    fun execStreaming(
        containerId: String,
        command: List<String>
    ): Flow<String>? {
        val container = runningProcesses[containerId]?.let {
            // Container is running, would need different approach
            return null
        }

        // For non-running containers, start fresh execution
        return scope.run {
            val containerResult = runBlocking {
                containerRepository.getContainerById(containerId)
            }

            containerResult?.let { cont ->
                val rootfsDir = containerRepository.getContainerRootfs(containerId)
                prootEngine.execStreaming(cont, rootfsDir, command)
            }
        }
    }

}