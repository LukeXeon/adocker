package com.github.andock.daemon.containers

import kotlinx.serialization.Serializable

@Serializable
data class ContainerLogLine(
    val timestamp: Long,
    val error: Boolean,
    val message: String
)