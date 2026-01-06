package com.github.andock.daemon.server.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContainerInspectResponse(
    @SerialName("Id")
    val id: String,
    @SerialName("Created")
    val created: String,
    @SerialName("Path")
    val path: String,
    @SerialName("Args")
    val args: List<String>,
    @SerialName("State")
    val state: ContainerStateInfo,
    @SerialName("Image")
    val image: String,
    @SerialName("Name")
    val name: String,
    @SerialName("Config")
    val config: ContainerConfigInfo,
    @SerialName("HostConfig")
    val hostConfig: HostConfig,
    @SerialName("NetworkSettings")
    val networkSettings: NetworkSettings
)