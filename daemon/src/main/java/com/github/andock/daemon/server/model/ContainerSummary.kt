package com.github.andock.daemon.server.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContainerSummary(
    @SerialName("Id")
    val id: String,
    @SerialName("Names")
    val names: List<String>,
    @SerialName("Image")
    val image: String,
    @SerialName("ImageID")
    val imageID: String,
    @SerialName("Command")
    val command: String,
    @SerialName("Created")
    val created: Long,
    @SerialName("State")
    val state: String,
    @SerialName("Status")
    val status: String,
    @SerialName("Ports")
    val ports: List<Port> = emptyList(),
    @SerialName("Labels")
    val labels: Map<String, String> = emptyMap(),
    @SerialName("SizeRw")
    val sizeRw: Long? = null,
    @SerialName("SizeRootFs")
    val sizeRootFs: Long? = null
)