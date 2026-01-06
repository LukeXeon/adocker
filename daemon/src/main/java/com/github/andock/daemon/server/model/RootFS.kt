package com.github.andock.daemon.server.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RootFS(
    @SerialName("Type")
    val type: String,
    @SerialName("Layers")
    val layers: List<String> = emptyList()
)