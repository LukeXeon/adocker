package com.github.adocker.core.registry.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class History(
    val created: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("empty_layer") val emptyLayer: Boolean? = null,
    val comment: String? = null
)