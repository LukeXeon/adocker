package com.github.adocker.daemon.containers

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Singleton

class ContainerProcess @AssistedInject constructor(
    @Assisted
    private val process: Process,
    scope: CoroutineScope
) {
    val job = scope.launch {
        try {
            runInterruptible {
                process.waitFor()
            }
        } catch (e: CancellationException) {
            process.destroy()
            process.waitFor()
            throw e
        }
    }

    val exitCode: Int
        get() = process.exitValue()

    val stdin: OutputStream
        get() = process.outputStream

    val stdout: InputStream
        get() = process.inputStream

    val stderr: InputStream
        get() = process.errorStream

    @Singleton
    @AssistedFactory
    interface Factory {
        fun create(@Assisted process: Process): ContainerProcess
    }
}