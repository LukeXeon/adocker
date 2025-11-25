package com.github.adocker.core.registry.model

import kotlinx.serialization.Serializable

@Serializable
data class ImageManifestV2(
    val schemaVersion: Int,
    val mediaType: String? = null,
    val config: ConfigDescriptor,
    val layers: List<LayerDescriptor>
)