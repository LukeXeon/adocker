package com.adocker.runner.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Local image stored on device
 */
@Serializable
data class LocalImage(
    val id: String = UUID.randomUUID().toString(),
    val repository: String,
    val tag: String,
    val digest: String,
    val architecture: String,
    val os: String,
    val created: Long = System.currentTimeMillis(),
    val size: Long = 0,
    val layerIds: List<String> = emptyList(),
    val config: ImageConfig? = null
) {
    val fullName: String get() = "$repository:$tag"
}