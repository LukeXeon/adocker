package com.github.adocker.daemon.containers

import android.system.Os
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import java.io.InputStream
import java.io.OutputStream

@OptIn(DelicateCoroutinesApi::class)
data class ContainerProcess(
    private val process: Process
) {
    val job = GlobalScope.launch(Dispatchers.IO) {
        try {
            runInterruptible {
                process.waitFor()
            }
        } catch (e: CancellationException) {
            process.destroy()
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
}