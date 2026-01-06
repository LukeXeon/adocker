package com.github.andock.daemon.containers

import java.io.BufferedWriter

sealed interface ContainerState {

    val id: String

    class Created(
        override val id: String
    ) : ContainerState

    data class Starting(
        override val id: String
    ) : ContainerState

    data class Running(
        override val id: String,
        val mainProcess: Process,
        val input: BufferedWriter,
        val childProcesses: List<Process> = emptyList(),
    ) : ContainerState

    data class Stopping(
        override val id: String,
        val processes: List<Process>,
    ) : ContainerState

    data class Removing(
        override val id: String
    ) : ContainerState

    data class Exited(
        override val id: String,
    ) : ContainerState

    data class Dead(
        override val id: String,
        val throwable: Throwable,
    ) : ContainerState

    data class Removed(
        override val id: String
    ) : ContainerState
}