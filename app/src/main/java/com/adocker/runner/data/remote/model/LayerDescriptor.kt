package com.adocker.runner.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class LayerDescriptor(
    val mediaType: String,
    val digest: String,
    val size: Long,
    val urls: List<String>? = null
)