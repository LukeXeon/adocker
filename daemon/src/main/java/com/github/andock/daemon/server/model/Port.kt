package com.github.andock.daemon.server.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Port(
    @SerialName("IP")
    val ip: String? = null,
    @SerialName("PrivatePort")
    val privatePort: Int,
    @SerialName("PublicPort")
    val publicPort: Int? = null,
    @SerialName("Type")
    val type: String
)