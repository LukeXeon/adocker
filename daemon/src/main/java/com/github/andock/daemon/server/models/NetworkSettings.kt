package com.github.andock.daemon.server.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkSettings(
    @SerialName("Bridge")
    val bridge: String = "",
    @SerialName("Gateway")
    val gateway: String = "",
    @SerialName("IPAddress")
    val ipAddress: String = "",
    @SerialName("IPPrefixLen")
    val ipPrefixLen: Int = 0,
    @SerialName("MacAddress")
    val macAddress: String = "",
    @SerialName("Networks")
    val networks: Map<String, NetworkInfo> = emptyMap()
)