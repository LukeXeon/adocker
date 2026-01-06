package com.github.andock.daemon.server.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExecInspectResponse(
    @SerialName("ID")
    val id: String,
    @SerialName("Running")
    val running: Boolean,
    @SerialName("ExitCode")
    val exitCode: Int,
    @SerialName("ProcessConfig")
    val processConfig: ProcessConfig,
    @SerialName("OpenStdin")
    val openStdin: Boolean,
    @SerialName("OpenStderr")
    val openStderr: Boolean,
    @SerialName("OpenStdout")
    val openStdout: Boolean,
    @SerialName("CanRemove")
    val canRemove: Boolean,
    @SerialName("ContainerID")
    val containerID: String,
    @SerialName("DetachKeys")
    val detachKeys: String,
    @SerialName("Pid")
    val pid: Int
)