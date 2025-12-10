package com.github.adocker.daemon.containers

import kotlinx.coroutines.CompletableDeferred

sealed interface SubProcessState {
    data class Creating(
        val containerId: String,
        val command: List<String>,
        val deferred: CompletableDeferred<Process>
    ) : SubProcessState

    data class Running(val process: Process) : SubProcessState
    data class Exited(val process: Process) : SubProcessState

    data class Error(val throwable: Throwable) : SubProcessState
}