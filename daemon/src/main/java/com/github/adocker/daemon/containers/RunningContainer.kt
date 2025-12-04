package com.github.adocker.daemon.containers

import androidx.annotation.WorkerThread
import com.github.adocker.daemon.app.AppConfig
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

class RunningContainer @AssistedInject constructor(
    @Assisted val context: ContainerContext,
    @Assisted val mainProcess: Process,
    appConfig: AppConfig,
    scope: CoroutineScope,
) {
    val containerId
        get() = context.containerId
    private val logDir = File(appConfig.logDir, containerId)
    val stdout = File(logDir, AppConfig.STDOUT)
    val stderr = File(logDir, AppConfig.STDERR)

    val stdin = mainProcess.outputStream.bufferedWriter()
    private val mutex = Mutex()
    private val otherProcesses = ArrayList<Process>()
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
            withContext(NonCancellable) {
                mainProcess.destroy()
                jobs.joinAll()
                mutex.withLock {
                    otherProcesses.forEach {
                        it.destroy()
                    }
                    otherProcesses.clear()
                }
            }
        }
    }

    suspend fun execCommand(command: List<String>): Result<Process> {
        mutex.withLock {
            if (!job.isActive) {
                return Result.failure(IllegalStateException("The container has stopped: $containerId"))
            }
            val process = context.startProcess(command)
            if (process.isSuccess) {
                otherProcesses.add(process.getOrThrow())
            }
            return process
        }
    }

    @Singleton
    class Launcher @Inject constructor(
        private val contextFactory: ContainerContext.Factory,
        private val containerFactory: Factory,
    ) {
        suspend fun start(containerId: String): RunningContainer {
            val context = contextFactory.create(containerId)
            val process = context.startProcess().getOrThrow()
            return containerFactory.create(context, process)
        }
    }

    @Singleton
    @AssistedFactory
    interface Factory {
        @WorkerThread
        fun create(
            @Assisted context: ContainerContext,
            @Assisted mainProcess: Process,
        ): RunningContainer
    }
}