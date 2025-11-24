package com.adocker.runner.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ManifestDescriptor(
    val mediaType: String,
    val digest: String,
    val size: Long,
    val platform: Platform? = null
)