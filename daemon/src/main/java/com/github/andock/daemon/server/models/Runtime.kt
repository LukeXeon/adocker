package com.github.andock.daemon.server.models

import kotlinx.serialization.Serializable

@Serializable
data class Runtime(
    val path: String,
    val runtimeArgs: List<String>?
)