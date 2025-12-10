package com.github.adocker.daemon.containers

import kotlinx.coroutines.Job
import java.io.BufferedWriter
import java.io.File

sealed interface ContainerState {
    val containerId: String

    class Created(
        override val containerId: String
    ) : ContainerState

    data class Starting(
        override val containerId: String
    ) : ContainerState

    data class Running(
        override val containerId: String,
        val job: Job,
        val stdin: BufferedWriter,
        val stdout: File,
        val stderr: File,
        val subProcesses: Set<Process> = emptySet(),
    ) : ContainerState

    data class Stopping(
        override val containerId: String,
        val job: Job,
        val subProcesses: Set<Process>,
    ) : ContainerState

    data class Removing(
        override val containerId: String
    ) : ContainerState

    data class Exited(
        override val containerId: String,
    ) : ContainerState

    data class Dead(
        override val containerId: String,
        val throwable: Throwable,
    ) : ContainerState

    data class Terminated(
        override val containerId: String
    ) : ContainerState
}