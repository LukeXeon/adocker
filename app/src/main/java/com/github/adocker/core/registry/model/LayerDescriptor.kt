package com.github.adocker.core.registry.model

import kotlinx.serialization.Serializable

@Serializable
data class LayerDescriptor(
    val mediaType: String,
    val digest: String,
    val size: Long,
    val urls: List<String>? = null
)