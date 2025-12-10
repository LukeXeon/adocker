package com.github.adocker.daemon.containers

import java.io.BufferedWriter
import java.io.File

sealed class ContainerState() {
    class Created(
        val containerId: String
    ) : ContainerState()

    data class Starting(
        val containerId: String
    ) : ContainerState()

    data class Running(
        val containerId: String,
        val mainProcess: Process,
        val stdin: BufferedWriter,
        val stdout: File,
        val stderr: File,
        val otherProcesses: List<Process>,
    ) : ContainerState()

    data class Stopping(
        val containerId: String,
        val processes: List<Process>,
    ) : ContainerState()

    data class Removing(
        val containerId: String
    ) : ContainerState()

    data class Exited(
        val containerId: String,
    ) : ContainerState()

    data class Dead(
        val containerId: String,
        val throwable: Throwable,
    ) : ContainerState()

    data class Terminated(
        val throwable: Throwable? = null,
    ) : ContainerState()
}