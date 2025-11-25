package com.github.adocker.core.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Rootfs(
    val type: String,
    @SerialName("diff_ids") val diffIds: List<String>
)