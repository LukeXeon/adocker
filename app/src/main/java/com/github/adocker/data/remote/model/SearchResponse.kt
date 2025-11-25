package com.github.adocker.data.remote.model

import kotlinx.serialization.Serializable

// Docker Hub Search API
@Serializable
data class SearchResponse(
    val count: Int,
    val next: String? = null,
    val previous: String? = null,
    val results: List<SearchResult>
)