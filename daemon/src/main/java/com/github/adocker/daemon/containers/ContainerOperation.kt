package com.github.adocker.daemon.containers

import com.github.adocker.daemon.os.JobProcess
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