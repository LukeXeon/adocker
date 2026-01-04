package com.github.andock.daemon.containers

import kotlinx.coroutines.CompletableDeferred

sealed interface ContainerOperation {

    object Start : ContainerOperation

    object Stop : ContainerOperation

    object Remove : ContainerOperation
    data class Exec(
        val command: List<String>,
        val process: CompletableDeferred<Process>
    ) : ContainerOperation
}