package com.github.adocker.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Docker Registry API DTOs
 */

@Serializable
data class AuthTokenResponse(
    val token: String? = null,
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("expiresIn") val expiresIn1: Int? = null,
    @SerialName("expires_in") val expiresIn2: Int? = null,
    @SerialName("issued_at") val issuedAt: String? = null
) {
    val expiresIn: Int
        get() {
            return expiresIn1 ?: expiresIn2 ?: 300
        }
}

