package com.github.andock.daemon.server.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExecCreateResponse(
    @SerialName("Id")
    val id: String
)