package com.github.adocker.daemon.registry.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Authentication Token Response
 *
 * Response from Docker Registry authentication/token service.
 * Handles both "token" and "access_token" field names for compatibility.
 * Conforms to: Docker Registry Token Authentication Specification
 *
 * @property token Bearer token for registry authentication (some registries use this field)
 * @property accessToken Bearer token for registry authentication (some registries use this field)
 * @property expiresIn1 Token expiration time in seconds (variant 1: "expiresIn")
 * @property expiresIn2 Token expiration time in seconds (variant 2: "expires_in")
 * @property issuedAt RFC 3339 formatted timestamp when the token was issued
 */
@Serializable
data class AuthTokenResponse(
    val token: String? = null,
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("expiresIn") val expiresIn1: Int? = null,
    @SerialName("expires_in") val expiresIn2: Int? = null,
    @SerialName("issued_at") val issuedAt: String? = null
) {
    /**
     * Get the token expiration time in seconds.
     * Checks both field name variants and defaults to 300 seconds (5 minutes).
     */
    val expiresIn: Int
        get() {
            return expiresIn1 ?: expiresIn2 ?: 300
        }
}

