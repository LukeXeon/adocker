package com.adocker.runner.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class ConfigDescriptor(
    val mediaType: String,
    val digest: String,
    val size: Long
)