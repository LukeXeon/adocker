package com.github.adocker.daemon.containers

import kotlinx.coroutines.CompletableDeferred
import java.io.File

sealed interface ProcessState {
    data class Starting(
        val containerId: String,
        val command: List<String>,
        val deferred: CompletableDeferred<Process>? = null,
        val stdout: File? = null,
        val stderr: File? = null,
    ) : ProcessState

    data class Running(
        val process: Process,
        val stdout: File? = null,
        val stderr: File? = null,
    ) : ProcessState

    data class Exited(val process: Process) : ProcessState
    data class Abort(val throwable: Throwable) : ProcessState
}