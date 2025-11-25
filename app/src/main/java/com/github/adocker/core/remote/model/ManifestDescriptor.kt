package com.github.adocker.core.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class ManifestDescriptor(
    val mediaType: String,
    val digest: String,
    val size: Long,
    val platform: Platform? = null
)