package com.github.adocker.daemon.containers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Container executor - manages container lifecycle and execution
 */
@Singleton
class ContainerExecutor @Inject constructor(
    private val factory: RunningContainer.Factory,
) {
    private val mutex = Mutex()
    private val runningContainers = MutableStateFlow<Map<String, RunningContainer>>(emptyMap())

    fun getAllRunningContainers(): Flow<List<RunningContainer>> {
        return runningContainers.map {
            it.values.toList()
        }
    }

    suspend fun startContainer(
        containerId: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            mutex.withLock {
                if (runningContainers.value[containerId]?.isActive == true) {
                    throw IllegalStateException("Container is already running")
                }
                val runningContainer = factory.create(containerId)
                runningContainers.value = buildMap {
                    putAll(runningContainers.value)
                    put(containerId, runningContainer)
                }
            }
        }
    }

    /**
     * Stop a running container
     */
    suspend fun stopContainer(containerId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                mutex.withLock {
                    val container = runningContainers.value[containerId]
                        ?: throw IllegalStateException("Container is not running")
                    container.destroy()
                    runningContainers.value = buildMap {
                        putAll(runningContainers.value)
                        remove(containerId)
                    }
                }
            }
        }
}