package com.github.adocker.core.registry.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Layer History Entry
 *
 * Describes how and when a layer was created during the image build process.
 * Provides metadata for image inspection and debugging.
 *
 * @property created RFC 3339 formatted timestamp when the layer was created
 * @property createdBy Command or instruction that created this layer (e.g., "RUN apt-get update")
 * @property emptyLayer True if this history entry doesn't correspond to a filesystem change
 * @property comment Optional custom message or annotation for this layer
 */
@Serializable
data class History(
    val created: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("empty_layer") val emptyLayer: Boolean? = null,
    val comment: String? = null
)