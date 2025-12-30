package com.github.andock.daemon.search

/**
 * Search parameters for Docker Hub repository search API.
 *
 * API Endpoint: https://hub.docker.com/v2/search/repositories/
 */
data class SearchParameters(
    val query: String,
    val pageSize: Int = 25,
    val page: Int = 1,
    val isOfficialOnly: Boolean,
    val type: String = "image"
)