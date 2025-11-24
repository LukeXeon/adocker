package com.adocker.runner.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RootfsDto(
    val type: String,
    @SerialName("diff_ids") val diffIds: List<String>
)