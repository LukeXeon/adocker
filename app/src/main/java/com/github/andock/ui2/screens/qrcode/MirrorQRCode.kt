package com.github.andock.ui2.screens.qrcode

import kotlinx.serialization.Serializable

/**
 * QR Code format for Docker Registry Mirror configuration
 *
 * Example JSON:
 * {
 *   "name": "My Mirror",
 *   "url": "https://registry.example.com",
 *   "bearerToken": "your-token-here"
 * }
 */
@Serializable
data class MirrorQRCode(
    val name: String,
    val url: String,
    val bearerToken: String? = null
)