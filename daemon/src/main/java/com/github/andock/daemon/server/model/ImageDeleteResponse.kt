package com.github.andock.daemon.server.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ImageDeleteResponse(
    @SerialName("Untagged")
    val untagged: String? = null,
    @SerialName("Deleted")
    val deleted: String? = null
)