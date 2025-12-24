package com.github.andock.daemon.client.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Docker Hub Search Result Item
 *
 * Represents a single image repository returned from Docker Hub's search API endpoint:
 * GET https://hub.docker.com/v2/search/repositories/?query={query}&page_size={limit}
 *
 * Note: This API is undocumented by Docker and field names/structure may change without notice.
 * Based on actual API responses as of 2024.
 *
 * @property repoName Full repository name (e.g., "xiaoxijin/apline", "alpine"), nullable to handle malformed responses
 * @property shortDescription Brief description of the repository
 * @property repoOwner Repository owner username (may be empty string for official images)
 * @property starCount Number of stars/favorites from users
 * @property pullCount Total number of times this image has been pulled
 * @property isOfficial True if this is an official Docker image
 * @property isAutomated True if this image has automated builds configured
 */
@Serializable
data class SearchResult(
    @SerialName("repo_name") val repoName: String? = null,
    @SerialName("short_description") val shortDescription: String? = null,
    @SerialName("repo_owner") val repoOwner: String? = null,
    @SerialName("star_count") val starCount: Int = 0,
    @SerialName("pull_count") val pullCount: Long = 0,
    @SerialName("is_official") val isOfficial: Boolean = false,
    @SerialName("is_automated") val isAutomated: Boolean = false
)