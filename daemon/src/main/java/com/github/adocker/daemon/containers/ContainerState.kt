package com.github.adocker.daemon.containers

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
        val mainProcess: ContainerProcess,
        val stdin: BufferedWriter,
        val stdout: File,
        val stderr: File,
        val childProcesses: Set<ContainerProcess> = emptySet(),
    ) : ContainerState

    data class Stopping(
        override val containerId: String,
        val mainProcess: ContainerProcess,
        val childProcesses: Set<ContainerProcess>,
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

    data class Removed(
        override val containerId: String
    ) : ContainerState
}