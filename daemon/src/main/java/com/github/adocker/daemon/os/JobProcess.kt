package com.github.adocker.daemon.os

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Singleton

class JobProcess @AssistedInject constructor(
    @Assisted
    private val process: Process,
    scope: CoroutineScope,
    waiter: ProcessWaiter,
) {
    val job = scope.async {
        try {
            waiter.waitFor(process)
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
        fun create(@Assisted process: Process): JobProcess
    }
}