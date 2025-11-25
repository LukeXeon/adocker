package com.github.adocker.engine.executor

import com.github.adocker.data.repository.ContainerRepository
import com.github.adocker.data.local.model.ContainerStatus
import com.github.adocker.engine.proot.ExecResult
import com.github.adocker.engine.proot.PRootEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Container executor - manages container lifecycle and execution
 * Equivalent to udocker's ExecutionEngine
 */
@Singleton
class ContainerExecutor @Inject constructor(
    private val prootEngine: PRootEngine,
    private val containerRepository: ContainerRepository
) {
    private val runningProcesses = mutableMapOf<String, ProcessHandle>()
    private val scope = CoroutineScope(Dispatchers.IO)

    data class ProcessHandle(
        val process: Process,
        val stdin: BufferedWriter,
        val stdout: BufferedReader,
        val job: Job?
    )

    private val _containerOutput = MutableStateFlow<Map<String, List<String>>>(emptyMap())

    val containerOutput: StateFlow<Map<String, List<String>>> = _containerOutput.asStateFlow()

    /**
     * Run a container (start and attach)
     */
    suspend fun runContainer(
        containerId: String,
        detach: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val container = containerRepository.getContainerById(containerId)
                ?: throw IllegalArgumentException("Container not found: $containerId")

            if (container.status == ContainerStatus.RUNNING) {
                throw IllegalStateException("Container is already running")
            }

            val rootfsDir = containerRepository.getContainerRootfs(containerId)
            if (!rootfsDir.exists()) {
                throw IllegalStateException("Container rootfs not found")
            }

            val process = prootEngine.startContainer(
                container = container,
                rootfsDir = rootfsDir,
            ).getOrThrow()

            val stdin = BufferedWriter(OutputStreamWriter(process.outputStream))
            val stdout = BufferedReader(InputStreamReader(process.inputStream))

            // Update container status - use hashCode as fallback for process identification
            containerRepository.setContainerRunning(containerId, process.hashCode())

            val job = if (!detach) {
                // Start output collection in background
                scope.launch {
                    collectOutput(containerId, stdout)
                }
            } else null

            runningProcesses[containerId] = ProcessHandle(process, stdin, stdout, job)

            // If not detached, wait for process to complete
            if (!detach) {
                val exitCode = process.waitFor()
                containerRepository.setContainerStopped(containerId)
                runningProcesses.remove(containerId)
            }
        }
    }

    /**
     * Start a container in the background
     */
    suspend fun startContainer(containerId: String): Result<Unit> {
        return runContainer(containerId, detach = true)
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
                    handle.process.destroyForcibly()
                } else {
                    handle.process.destroy()
                    // Give it some time to gracefully shutdown
                    val exited = handle.process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)
                    if (!exited) {
                        handle.process.destroyForcibly()
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
            val containerResult = kotlinx.coroutines.runBlocking {
                containerRepository.getContainerById(containerId)
            }

            containerResult?.let { cont ->
                val rootfsDir = containerRepository.getContainerRootfs(containerId)
                prootEngine.execStreaming(cont, rootfsDir, command)
            }
        }
    }

    /**
     * Send input to a running container
     */
    suspend fun sendInput(containerId: String, input: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val handle = runningProcesses[containerId]
                    ?: throw IllegalStateException("Container is not running")

                handle.stdin.write(input)
                if (!input.endsWith("\n")) {
                    handle.stdin.newLine()
                }
                handle.stdin.flush()
            }
        }

    /**
     * Check if container is running
     */
    fun isContainerRunning(containerId: String): Boolean {
        return runningProcesses[containerId]?.process?.isAlive == true
    }

    /**
     * Get container exit code (if stopped)
     */
    fun getExitCode(containerId: String): Int? {
        val handle = runningProcesses[containerId]
        return if (handle?.process?.isAlive == false) {
            handle.process.exitValue()
        } else null
    }

    /**
     * Collect output from container process
     */
    private suspend fun collectOutput(containerId: String, reader: BufferedReader) {
        withContext(Dispatchers.IO) {
            try {
                val lines = mutableListOf<String>()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let {
                        lines.add(it)
                        _containerOutput.value = _containerOutput.value.toMutableMap().apply {
                            put(containerId, lines.toList())
                        }
                    }
                }
            } catch (e: Exception) {
                // Stream closed
            }
        }
    }

    /**
     * Attach to a running container's output
     */
    fun attachToContainer(containerId: String): Flow<String>? {
        val handle = runningProcesses[containerId] ?: return null

        return kotlinx.coroutines.flow.flow {
            try {
                var line: String?
                while (handle.stdout.readLine().also { line = it } != null) {
                    line?.let { emit(it) }
                }
            } catch (e: Exception) {
                // Stream closed
            }
        }
    }

    /**
     * Kill all running containers
     */
    suspend fun killAll() = withContext(Dispatchers.IO) {
        runningProcesses.keys.toList().forEach { containerId ->
            stopContainer(containerId, force = true)
        }
    }

    /**
     * Get list of running container IDs
     */
    fun getRunningContainerIds(): List<String> {
        return runningProcesses.keys.toList()
    }
}
