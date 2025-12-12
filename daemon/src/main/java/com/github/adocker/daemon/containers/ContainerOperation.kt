package com.github.adocker.daemon.containers

import kotlin.coroutines.Continuation

sealed interface ContainerOperation {

    object Start : ContainerOperation

    object Stop : ContainerOperation

    object Remove : ContainerOperation
    data class Exec(
        val command: List<String>,
        val continuation: Continuation<ContainerProcess>
    ) : ContainerOperation
}