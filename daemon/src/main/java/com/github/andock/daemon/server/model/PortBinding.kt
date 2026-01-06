package com.github.andock.daemon.server.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PortBinding(
    @SerialName("HostIp")
    val hostIp: String? = null,
    @SerialName("HostPort")
    val hostPort: String
)