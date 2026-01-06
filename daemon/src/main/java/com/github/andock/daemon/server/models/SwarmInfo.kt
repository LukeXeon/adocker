package com.github.andock.daemon.server.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SwarmInfo(
    @SerialName("NodeID")
    val nodeID: String,
    @SerialName("NodeAddr")
    val nodeAddr: String,
    @SerialName("LocalNodeState")
    val localNodeState: String,
    @SerialName("ControlAvailable")
    val controlAvailable: Boolean,
    @SerialName("Error")
    val error: String,
    @SerialName("RemoteManagers")
    val remoteManagers: List<String>?
)