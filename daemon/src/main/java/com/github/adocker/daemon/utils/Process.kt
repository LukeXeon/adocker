package com.github.adocker.daemon.utils

import android.os.Build
import java.io.File

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



/**
 * Start an interactive process
 */
fun startProcess(
    command: List<String>,
    workingDir: File? = null,
    environment: Map<String, String> = emptyMap(),
    redirectErrorStream: Boolean = true
): Process {
    val processBuilder = ProcessBuilder(command).apply {
        workingDir?.let { directory(it) }
        environment().putAll(environment)
        redirectErrorStream(redirectErrorStream)
    }
    return processBuilder.start()
}
