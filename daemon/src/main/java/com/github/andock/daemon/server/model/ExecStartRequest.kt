package com.github.andock.daemon.server.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExecStartRequest(
    @SerialName("Detach")
    val detach: Boolean = false,
    @SerialName("Tty")
    val tty: Boolean = false
)