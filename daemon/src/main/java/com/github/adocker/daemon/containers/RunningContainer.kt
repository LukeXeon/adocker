package com.github.adocker.daemon.containers

import androidx.annotation.WorkerThread
import com.github.adocker.daemon.config.AppConfig
import com.github.adocker.daemon.utils.isActive
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.io.File
import javax.inject.Singleton

class RunningContainer @AssistedInject constructor(
    @Assisted val containerId: String,
    private val engine: PRootEngine,
    private val containerRepository: ContainerRepository,
    appConfig: AppConfig,
) {
    val containerDir = File(appConfig.containersDir, containerId)
    val rootfsDir = File(containerDir, AppConfig.Companion.ROOTFS_DIR)

    private val lock = Any()
    private var isDestroyed = false

    private fun startProcess(command: List<String>? = null): Result<Process> {
        if (!rootfsDir.exists()) {
            return Result.failure(IllegalStateException("Container rootfs not found"))
        }
        return engine.startProcess(
            checkNotNull(
                containerRepository.getContainerById(containerId)
            ) { "Container not found: $containerId" }.config,
            rootfsDir,
            command
        )
    }

    private val mainProcess = startProcess().getOrThrow()

    private val otherProcesses = ArrayList<Process>()

    fun execCommand(command: List<String>): Result<Process> {
        synchronized(lock) {
            if (isDestroyed) {
                return Result.failure(IllegalStateException("The container has stopped: $containerId"))
            }
            val process = startProcess(command)
            if (process.isSuccess) {
                otherProcesses.add(process.getOrThrow())
            }
            return process
        }
    }

    val isActive: Boolean
        get() = synchronized(lock) {
            !isDestroyed && mainProcess.isActive
        }

    fun destroy() {
        synchronized(lock) {
            if (!isDestroyed) {
                isDestroyed = true
                mainProcess.destroy()
                synchronized(otherProcesses) {
                    otherProcesses.forEach {
                        it.destroy()
                    }
                }
            }
        }
    }

    @Singleton
    @AssistedFactory
    interface Factory {
        @WorkerThread
        fun create(@Assisted containerId: String): RunningContainer
    }
}