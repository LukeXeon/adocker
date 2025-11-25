package com.github.adocker.core.registry.model

import kotlinx.serialization.Serializable

/**
 * Manifest List (Fat Manifest)
 *
 * References platform-specific image manifests, enabling multi-architecture support.
 * Also handles backwards compatibility with V2 Schema 1 manifests.
 * Conforms to: https://distribution.github.io/distribution/spec/manifest-v2-2/
 *
 * @property schemaVersion Must be 2 for this schema version
 * @property mediaType Should be "application/vnd.docker.distribution.manifest.list.v2+json"
 * @property manifests Array of platform-specific manifest references (for manifest list)
 * @property config Image configuration descriptor (for V2 Schema 1 backward compatibility)
 * @property layers Layer descriptors (for V2 Schema 1 backward compatibility)
 */
@Serializable
data class ManifestListResponse(
    val schemaVersion: Int,
    val mediaType: String? = null,
    val manifests: List<ManifestDescriptor>? = null,
    // For v2 schema 1
    val config: ConfigDescriptor? = null,
    val layers: List<LayerDescriptor>? = null
)