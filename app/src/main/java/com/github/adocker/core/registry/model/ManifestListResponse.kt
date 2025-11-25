package com.github.adocker.core.registry.model

import kotlinx.serialization.Serializable

@Serializable
data class ManifestListResponse(
    val schemaVersion: Int,
    val mediaType: String? = null,
    val manifests: List<ManifestDescriptor>? = null,
    // For v2 schema 1
    val config: ConfigDescriptor? = null,
    val layers: List<LayerDescriptor>? = null
)