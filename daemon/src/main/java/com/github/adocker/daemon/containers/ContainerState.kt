package com.github.adocker.daemon.containers

import kotlinx.coroutines.Job
import java.io.BufferedWriter
import java.io.File

sealed interface ContainerState {
    class Created(
        val containerId: String
    ) : ContainerState

    data class Starting(
        val containerId: String
    ) : ContainerState

    data class Running(
        val containerId: String,
        val mainProcess: ContainerProcess,
        val stdin: BufferedWriter,
        val stdout: File,
        val stderr: File,
        val childProcesses: Set<ContainerProcess> = emptySet(),
    ) : ContainerState

    data class Stopping(
        val containerId: String,
        val processes: List<Job>,
    ) : ContainerState

    data class Removing(
        val containerId: String
    ) : ContainerState

    data class Exited(
        val containerId: String,
    ) : ContainerState

    data class Dead(
        val containerId: String,
        val throwable: Throwable,
    ) : ContainerState

    data class Removed(
        val containerId: String
    ) : ContainerState
}