package com.github.andock.daemon.images.model

import kotlinx.serialization.Serializable

/**
 * Volume binding (host:container)
 */
@Serializable
data class VolumeBinding(
    val hostPath: String,
    val containerPath: String,
    val readOnly: Boolean = false
)