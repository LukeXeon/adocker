package com.github.andock.ui.screens.registries

import kotlinx.serialization.Serializable

@Serializable
data class MirrorQrcode(
    val name: String,
    val url: String,
    val token: String?,
    val priority: Int
)