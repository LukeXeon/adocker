package com.github.andock.daemon.client.model

import kotlinx.serialization.Serializable

/**
 * Tags List Response
 *
 * Response from the Docker Registry API V2 tags list endpoint.
 * Returns all available tags for a given repository.
 * API Endpoint: GET /v2/<name>/tags/list
 *
 * @property name Repository name
 * @property tags List of available tag names for this repository
 */
@Serializable
data class TagsListResponse(
    val name: String,
    val tags: List<String>
)