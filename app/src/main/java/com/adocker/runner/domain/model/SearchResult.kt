package com.adocker.runner.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Search result from Docker Hub
 */
@Serializable
data class SearchResult(
    val name: String,
    val description: String = "",
    @SerialName("star_count") val starCount: Int = 0,
    @SerialName("is_official") val isOfficial: Boolean = false,
    @SerialName("is_automated") val isAutomated: Boolean = false
)