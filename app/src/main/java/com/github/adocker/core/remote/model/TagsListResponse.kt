package com.github.adocker.core.remote.model

import kotlinx.serialization.Serializable

// Tags API
@Serializable
data class TagsListResponse(
    val name: String,
    val tags: List<String>
)