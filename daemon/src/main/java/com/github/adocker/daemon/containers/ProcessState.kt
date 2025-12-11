package com.github.adocker.daemon.containers

import kotlinx.coroutines.CompletableDeferred

sealed interface ProcessState {
    data class Starting(
        val containerId: String,
        val command: List<String>,
        val deferred: CompletableDeferred<Process>
    ) : ProcessState

    data class Running(val process: Process) : ProcessState
    data class Exited(val process: Process) : ProcessState
    data class Abort(val throwable: Throwable) : ProcessState
}