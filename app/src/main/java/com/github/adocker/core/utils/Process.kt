package com.github.adocker.core.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.sequences.forEach
import kotlin.time.DurationUnit
import kotlin.time.toDuration

suspend fun Process.awaitExitValue(
    timeout: Long,
    unit: DurationUnit = DurationUnit.MILLISECONDS
): Int = withContext(Dispatchers.IO) {
    if (timeout > 0) {
        val code = withTimeoutOrNull(timeout.toDuration(unit)) {
            var code: Int = -1
            while (isActive) {
                try {
                    code = exitValue()
                    break
                } catch (_: IllegalThreadStateException) {
                    // Process is still running
                    delay(100)
                }
            }
            return@withTimeoutOrNull code
        }
        if (code != null) {
            return@withContext code
        } else {
            destroy()
            return@withContext -1
        }
    } else {
        return@withContext waitFor()
    }
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
    val stdout = StringBuilder()
    val stderr = StringBuilder()
    // Read stdout
    val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
    val stderrReader = BufferedReader(InputStreamReader(process.errorStream))
    val stdoutThread = launch {
        stdoutReader.useLines { lines ->
            lines.forEach { stdout.appendLine(it) }
        }
    }
    val stderrThread = launch {
        stderrReader.useLines { lines ->
            lines.forEach { stderr.appendLine(it) }
        }
    }
    val exitCode = process.awaitExitValue(timeout)
    stdoutThread.join()
    stderrThread.join()
    ProcessResult(
        exitCode = exitCode,
        stdout = stdout.toString().trim(),
        stderr = stderr.toString().trim()
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