package com.github.andock.daemon.app

import kotlinx.serialization.Serializable

@Serializable
data class AppTaskInfo(
    val name: String,
    val trigger: String
)
