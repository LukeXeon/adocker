package com.github.adocker.daemon.containers

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.runInterruptible
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.CoroutineContext

@OptIn(DelicateCoroutinesApi::class)
data class ContainerProcess(
    private val process: Process
) {
    val job = GlobalScope.async(Dispatchers.IO) {
        try {
            runInterruptible {
                process.waitFor()
            }
        } catch (e: CancellationException) {
            process.destroy()
            throw e
        }
    }
    val stdin: OutputStream
        get() = process.outputStream

    val stdout: InputStream
        get() = process.inputStream

    val stderr: InputStream
        get() = process.errorStream
}