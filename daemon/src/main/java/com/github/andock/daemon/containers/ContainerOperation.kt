package com.github.andock.daemon.containers

import com.github.andock.daemon.os.JobProcess
import kotlinx.coroutines.CompletableDeferred

sealed interface ContainerOperation {

    object Start : ContainerOperation

    object Stop : ContainerOperation

    object Remove : ContainerOperation
    data class Exec(
        val command: List<String>,
        val process: CompletableDeferred<JobProcess>
    ) : ContainerOperation
}