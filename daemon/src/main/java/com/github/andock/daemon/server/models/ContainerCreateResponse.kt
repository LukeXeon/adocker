package com.github.andock.daemon.server.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContainerCreateResponse(
    @SerialName("Id")
    val id: String,
    @SerialName("Warnings")
    val warnings: List<String>? = null
)