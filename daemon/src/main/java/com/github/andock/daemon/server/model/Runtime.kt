package com.github.andock.daemon.server.model

import kotlinx.serialization.Serializable

@Serializable
data class Runtime(
    val path: String,
    val runtimeArgs: List<String>?
)