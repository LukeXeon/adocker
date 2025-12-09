package com.github.adocker.daemon.containers

import com.github.adocker.daemon.app.AppContext
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
import javax.inject.Singleton

class RunningContainer @AssistedInject constructor(
    @Assisted val context: ContainerContext,
    @Assisted internal val mainProcess: Process,
    appContext: AppContext,
    scope: CoroutineScope,
) {
    val containerId
        get() = context.containerId
    private val logDir = File(appContext.logDir, containerId)
    val stdout = File(logDir, AppContext.STDOUT)
    val stderr = File(logDir, AppContext.STDERR)

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
            val result = context.startProcess(command)
            val process = result.getOrNull()
            if (process != null) {
                otherProcesses.add(process)
            }
            return result
        }
    }

    @Singleton
    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted context: ContainerContext,
            @Assisted mainProcess: Process,
        ): RunningContainer
    }
}