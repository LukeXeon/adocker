package com.github.andock.daemon.server.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkInfo(
    @SerialName("NetworkID")
    val networkID: String,
    @SerialName("EndpointID")
    val endpointID: String,
    @SerialName("Gateway")
    val gateway: String,
    @SerialName("IPAddress")
    val ipAddress: String,
    @SerialName("IPPrefixLen")
    val ipPrefixLen: Int,
    @SerialName("MacAddress")
    val macAddress: String
)