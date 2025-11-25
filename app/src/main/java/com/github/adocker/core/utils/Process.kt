package com.github.adocker.core.utils

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Forcibly kills the process.
 * Compatibility wrapper for Process.destroyForcibly() (API 26+)
 */
fun Process.destroyForciblyCompat(): Process {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // API 26+: Use the native method
        return destroyForcibly()
    } else {
        // API < 26: Fallback implementation
        destroy()
        return this
    }
}

/**
 * Checks if the process is still running.
 * Compatibility wrapper for Process.isAlive() (API 26+)
 */
val Process.isActive: Boolean
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        isAlive
    } else {
        try {
            exitValue()
            false // Process has exited
        } catch (_: IllegalThreadStateException) {
            true // Process is still running
        }
    }

suspend fun Process.await(
    timeout: Long,
    unit: DurationUnit = DurationUnit.MILLISECONDS
): Boolean = withContext(Dispatchers.IO) {
    withTimeoutOrNull(timeout.toDuration(unit)) {
        var finished = false
        while (isActive) {
            try {
                exitValue()
                finished = true
                break
            } catch (_: IllegalThreadStateException) {
                // Process is still running
                delay(100)
            }
        }
        return@withTimeoutOrNull finished
    } ?: false
}

/**
 * Execute a command and wait for completion
 */
suspend fun execute(
    command: List<String>,
    workingDir: File? = null,
    environment: Map<String, String> = emptyMap(),
    timeout: Long = 0
): ProcessResult = withContext(Dispatchers.IO) {
    val processBuilder = ProcessBuilder(command).apply {
        workingDir?.let { directory(it) }
        environment().putAll(environment)
        redirectErrorStream(false)
    }
    val process = processBuilder.start()
    // Read
    val jobs = sequenceOf(
        process.inputStream,
        process.errorStream
    ).map {
        BufferedReader(InputStreamReader(it))
    }.map { reader ->
        async {
            reader.useLines { lines ->
                val builder = StringBuilder()
                lines.forEach { builder.appendLine(it) }
                return@useLines builder
            }
        }
    }.toList()
    val exitCode = if (timeout > 0) {
        val finished = process.await(timeout, DurationUnit.MILLISECONDS)
        if (!finished) {
            process.destroyForciblyCompat()
            -1
        } else {
            process.exitValue()
        }
    } else {
        process.waitFor()
    }
    val outputs = jobs.awaitAll()
    ProcessResult(
        exitCode = exitCode,
        stdout = outputs[0].toString().trim(),
        stderr = outputs[1].toString().trim()
    )
}

/**
 * Execute a command and stream output line by line
 */
fun executeStreaming(
    command: List<String>,
    workingDir: File? = null,
    environment: Map<String, String> = emptyMap()
): Flow<String> = flow {
    val processBuilder = ProcessBuilder(command).apply {
        workingDir?.let { directory(it) }
        environment().putAll(environment)
        redirectErrorStream(true)
    }
    val process = processBuilder.start()
    BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
        while (true) {
            val line = reader.readLine()
            if (line == null) {
                break
            } else {
                emit(line)
            }
        }
    }
    process.waitFor()
}.flowOn(Dispatchers.IO)

/**
 * Start an interactive process
 */
fun startInteractiveProcess(
    command: List<String>,
    workingDir: File? = null,
    environment: Map<String, String> = emptyMap()
): Process {
    val processBuilder = ProcessBuilder(command).apply {
        workingDir?.let { directory(it) }
        environment().putAll(environment)
        redirectErrorStream(true)
    }
    return processBuilder.start()
}

val ProcessResult.isSuccess: Boolean
    get() = exitCode == 0