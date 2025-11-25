package com.github.adocker.data.remote.model

import kotlinx.serialization.Serializable

// Tags API
@Serializable
data class TagsListResponse(
    val name: String,
    val tags: List<String>
)