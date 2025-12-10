package com.github.adocker.daemon.containers

import com.github.adocker.daemon.registry.model.ContainerConfig
import kotlinx.coroutines.CompletableDeferred

sealed class ContainerOperation {

    object Start : ContainerOperation()

    object Stop : ContainerOperation()

    object Remove : ContainerOperation()
    data class Exec(
        val command: List<String>,
        val deferred: CompletableDeferred<Process>? = null
    ) : ContainerOperation()
}