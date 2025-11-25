package com.github.adocker.core.registry.model

import kotlinx.serialization.Serializable

// Tags API
@Serializable
data class TagsListResponse(
    val name: String,
    val tags: List<String>
)