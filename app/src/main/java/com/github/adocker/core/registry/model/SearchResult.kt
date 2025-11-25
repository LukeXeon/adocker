package com.github.adocker.core.registry.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Docker Hub Search Result Item
 *
 * Represents a single image repository in Docker Hub search results.
 * Contains metadata about the image's popularity and official status.
 *
 * @property name Repository name (e.g., "nginx", "ubuntu")
 * @property description Brief description of the image
 * @property starCount Number of stars/favorites from users
 * @property isOfficial True if this is an official Docker image
 * @property isAutomated True if this image has automated builds configured
 * @property pullCount Total number of times this image has been pulled
 */
@Serializable
data class SearchResult(
    val name: String,
    val description: String? = null,
    @SerialName("star_count") val starCount: Int = 0,
    @SerialName("is_official") val isOfficial: Boolean = false,
    @SerialName("is_automated") val isAutomated: Boolean = false,
    @SerialName("pull_count") val pullCount: Long = 0
)