package com.github.andock.daemon.server.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContainerStateInfo(
    @SerialName("Status")
    val status: String,
    @SerialName("Running")
    val running: Boolean,
    @SerialName("Paused")
    val paused: Boolean,
    @SerialName("Restarting")
    val restarting: Boolean,
    @SerialName("Dead")
    val dead: Boolean,
    @SerialName("Pid")
    val pid: Int,
    @SerialName("ExitCode")
    val exitCode: Int,
    @SerialName("Error")
    val error: String,
    @SerialName("StartedAt")
    val startedAt: String,
    @SerialName("FinishedAt")
    val finishedAt: String
)