package com.github.andock.daemon.server.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IndexConfig(
    @SerialName("Name")
    val name: String,
    @SerialName("Mirrors")
    val mirrors: List<String>,
    @SerialName("Secure")
    val secure: Boolean,
    @SerialName("Official")
    val official: Boolean
)