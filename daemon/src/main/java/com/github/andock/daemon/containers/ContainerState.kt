package com.github.andock.daemon.containers

import com.github.andock.daemon.os.JobProcess
import kotlinx.coroutines.Job
import java.io.BufferedWriter
import java.io.File

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
        val mainProcess: JobProcess,
        val stdin: BufferedWriter,
        val stdout: File,
        val stderr: File,
        val childProcesses: List<JobProcess> = emptyList(),
    ) : ContainerState

    data class Stopping(
        override val id: String,
        val processes: List<Job>,
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