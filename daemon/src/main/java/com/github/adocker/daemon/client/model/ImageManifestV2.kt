package com.github.adocker.daemon.client.model

import kotlinx.serialization.Serializable

/**
 * Docker Image Manifest V2, Schema 2
 *
 * Provides configuration and filesystem layers for a single container image for a specific architecture and OS.
 * Conforms to: https://distribution.github.io/distribution/spec/manifest-v2-2/
 *
 * @property schemaVersion Must be 2 for this schema version
 * @property mediaType Should be "application/vnd.docker.distribution.manifest.v2+json"
 * @property config References the container configuration blob (image config JSON)
 * @property layers Ordered array of filesystem layer descriptors (base layer first)
 */
@Serializable
data class ImageManifestV2(
    val schemaVersion: Int,
    val mediaType: String? = null,
    val config: ConfigDescriptor,
    val layers: List<LayerDescriptor>
)