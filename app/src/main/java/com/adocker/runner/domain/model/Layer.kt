package com.adocker.runner.domain.model

import kotlinx.serialization.Serializable

/**
 * Layer information
 */
@Serializable
data class Layer(
    val digest: String,
    val size: Long,
    val mediaType: String,
    val downloaded: Boolean = false,
    val extracted: Boolean = false
)