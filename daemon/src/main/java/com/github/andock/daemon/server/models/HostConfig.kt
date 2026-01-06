package com.github.andock.daemon.server.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HostConfig(
    @SerialName("Binds")
    val binds: List<String>? = null,
    @SerialName("PortBindings")
    val portBindings: Map<String, List<PortBinding>>? = null,
    @SerialName("NetworkMode")
    val networkMode: String? = null
)