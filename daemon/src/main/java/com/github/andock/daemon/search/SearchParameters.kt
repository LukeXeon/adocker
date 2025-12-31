package com.github.andock.daemon.search

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Search parameters for Docker Hub repository search API.
 *
 * API Endpoint: https://hub.docker.com/v2/search/repositories/
 */
@Serializable
data class SearchParameters(
    @SerialName("query")
    val query: String,
    @SerialName("is_official")
    val isOfficial: Boolean?,
    @SerialName("page")
    val page: Int,
    @SerialName("page_size")
    val pageSize: Int,
    @SerialName("type")
    val type: String,
) {
    constructor(query: String, isOfficial: Boolean?) : this(
        query, isOfficial, 1, 25, "image"
    )
}