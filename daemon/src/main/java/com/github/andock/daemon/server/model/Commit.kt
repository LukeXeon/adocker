package com.github.andock.daemon.server.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Commit(
    @SerialName("ID")
    val id: String,
    @SerialName("Expected")
    val expected: String
)