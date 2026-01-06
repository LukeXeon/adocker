package com.github.andock.daemon.server.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProcessConfig(
    @SerialName("tty")
    val tty: Boolean,
    @SerialName("entrypoint")
    val entrypoint: String,
    @SerialName("arguments")
    val arguments: List<String>,
    @SerialName("privileged")
    val privileged: Boolean = false,
    @SerialName("user")
    val user: String? = null
)