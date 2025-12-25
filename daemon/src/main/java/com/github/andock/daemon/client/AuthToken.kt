package com.github.andock.daemon.client

data class AuthToken(
    val token: String,
    val expiry: Long
)