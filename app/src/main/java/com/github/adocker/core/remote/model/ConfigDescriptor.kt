package com.github.adocker.core.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class ConfigDescriptor(
    val mediaType: String,
    val digest: String,
    val size: Long
)