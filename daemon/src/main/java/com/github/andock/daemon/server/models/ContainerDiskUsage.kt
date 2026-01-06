package com.github.andock.daemon.server.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContainerDiskUsage(
    @SerialName("Id")
    val id: String,
    @SerialName("Names")
    val names: List<String>,
    @SerialName("Image")
    val image: String,
    @SerialName("ImageID")
    val imageID: String,
    @SerialName("Created")
    val created: Long,
    @SerialName("SizeRw")
    val sizeRw: Long,
    @SerialName("SizeRootFs")
    val sizeRootFs: Long,
    @SerialName("State")
    val state: String,
    @SerialName("Status")
    val status: String
)