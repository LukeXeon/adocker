package com.github.adocker.daemon.containers

import androidx.annotation.WorkerThread
import com.github.adocker.daemon.config.AppConfig
import com.github.adocker.daemon.utils.isActive
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Singleton

class RunningContainer @AssistedInject constructor(
    @Assisted val containerId: String,
    private val engine: PRootEngine,
    private val containerRepository: ContainerRepository,
    appConfig: AppConfig,
    scope: CoroutineScope,
) {
    val containerDir = File(appConfig.containersDir, containerId)
    val rootfsDir = File(containerDir, AppConfig.Companion.ROOTFS_DIR)

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

    val input = mainProcess.inputStream.bufferedReader()
    val output = mainProcess.outputStream.bufferedWriter()

    init {
        scope.launch {
            mainProcess.waitFor()
            synchronized(otherProcesses) {
                otherProcesses.forEach {
                    it.destroy()
                }
                otherProcesses.clear()
            }
        }
    }

    fun execCommand(command: List<String>): Result<Process> {
        synchronized(otherProcesses) {
            if (mainProcess.isActive) {
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
        get() = mainProcess.isActive

    fun destroy() {
        mainProcess.destroy()
    }

    @Singleton
    @AssistedFactory
    interface Factory {
        @WorkerThread
        fun create(@Assisted containerId: String): RunningContainer
    }
}