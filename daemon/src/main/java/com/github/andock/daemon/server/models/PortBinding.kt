package com.github.andock.daemon.server.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PortBinding(
    @SerialName("HostIp")
    val hostIp: String? = null,
    @SerialName("HostPort")
    val hostPort: String
)