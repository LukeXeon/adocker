package com.adocker.runner.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class ImageManifestV2(
    val schemaVersion: Int,
    val mediaType: String? = null,
    val config: ConfigDescriptor,
    val layers: List<LayerDescriptor>
)