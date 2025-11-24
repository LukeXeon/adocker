package com.adocker.runner.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HistoryDto(
    val created: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("empty_layer") val emptyLayer: Boolean? = null,
    val comment: String? = null
)