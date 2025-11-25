package com.github.adocker.core.registry.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchResult(
    val name: String,
    val description: String? = null,
    @SerialName("star_count") val starCount: Int = 0,
    @SerialName("is_official") val isOfficial: Boolean = false,
    @SerialName("is_automated") val isAutomated: Boolean = false,
    @SerialName("pull_count") val pullCount: Long = 0
)