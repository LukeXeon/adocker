package com.github.adocker.daemon.utils

import java.io.File


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
