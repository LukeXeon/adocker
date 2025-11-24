package com.adocker.runner.core.utils

import android.os.Build
import android.system.Os
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Process execution utilities
 */
object ProcessUtils {

    data class ProcessResult(
        val exitCode: Int,
        val stdout: String,
        val stderr: String
    ) {
        val isSuccess: Boolean get() = exitCode == 0
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

        val stdoutThread = Thread {
            stdoutReader.useLines { lines ->
                lines.forEach { stdout.appendLine(it) }
            }
        }

        val stderrThread = Thread {
            stderrReader.useLines { lines ->
                lines.forEach { stderr.appendLine(it) }
            }
        }

        stdoutThread.start()
        stderrThread.start()

        val exitCode = if (timeout > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // API 26+: Use waitFor with timeout
                val finished = process.waitFor(timeout, java.util.concurrent.TimeUnit.MILLISECONDS)
                if (!finished) {
                    process.destroyForcibly()
                    -1
                } else {
                    process.exitValue()
                }
            } else {
                // API < 26: Manual timeout implementation
                val startTime = System.currentTimeMillis()
                var finished = false

                while (System.currentTimeMillis() - startTime < timeout) {
                    try {
                        process.exitValue()
                        finished = true
                        break
                    } catch (e: IllegalThreadStateException) {
                        // Process is still running
                        delay(100)
                    }
                }

                if (!finished) {
                    process.destroy()
                    -1
                } else {
                    process.exitValue()
                }
            }
        } else {
            process.waitFor()
        }

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
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                emit(line!!)
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

    /**
     * Check if a process is running
     */
    fun isProcessRunning(pid: Int): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("kill", "-0", pid.toString()))
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Kill a process by PID
     */
    suspend fun killProcess(pid: Int, force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        try {
            val signal = if (force) "-9" else "-15"
            val process = Runtime.getRuntime().exec(arrayOf("kill", signal, pid.toString()))
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
}
