package com.github.adocker.daemon.containers

import androidx.annotation.WorkerThread
import com.github.adocker.daemon.app.AppConfig
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
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
    val rootfsDir = File(containerDir, AppConfig.ROOTFS_DIR)

    val logDir = File(appConfig.logDir, containerId)
    private val mainProcess = startProcess().getOrThrow()
    private val otherProcesses = ArrayList<Process>()

    val stdin = mainProcess.outputStream.bufferedWriter()
    val stdout = File(logDir, AppConfig.STDOUT)
    val stderr = File(logDir, AppConfig.STDERR)
    val job = scope.launch {
        logDir.mkdirs()
        val jobs = mapOf(
            stdout to mainProcess.inputStream,
            stderr to mainProcess.errorStream
        ).map {
            val (file, stream) = it
            launch {
                stream.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }.toList()
        try {
            runInterruptible {
                mainProcess.waitFor()
            }
        } finally {
            mainProcess.destroy()
            jobs.joinAll()
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
            if (!job.isActive) {
                return Result.failure(IllegalStateException("The container has stopped: $containerId"))
            }
            val process = startProcess(command)
            if (process.isSuccess) {
                otherProcesses.add(process.getOrThrow())
            }
            return process
        }
    }

    private fun startProcess(command: List<String>? = null): Result<Process> {
        if (!rootfsDir.exists()) {
            return Result.failure(IllegalStateException("Container rootfs not found"))
        }
        return engine.startProcess(
            checkNotNull(
                containerRepository.getContainerByIdSync(containerId)
            ) { "Container not found: $containerId" }.config,
            rootfsDir,
            command
        )
    }

    @Singleton
    @AssistedFactory
    interface Factory {
        @WorkerThread
        fun create(@Assisted containerId: String): RunningContainer
    }
}