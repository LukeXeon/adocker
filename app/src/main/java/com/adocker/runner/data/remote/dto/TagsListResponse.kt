package com.adocker.runner.data.remote.dto

import kotlinx.serialization.Serializable

// Tags API
@Serializable
data class TagsListResponse(
    val name: String,
    val tags: List<String>
)