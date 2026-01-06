package com.github.andock.daemon.server.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Plugins(
    @SerialName("Volume")
    val volume: List<String>,
    @SerialName("Network")
    val network: List<String>,
    @SerialName("Authorization")
    val authorization: List<String>?,
    @SerialName("Log")
    val log: List<String>
)