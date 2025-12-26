package com.github.andock.daemon.database.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    "auth_tokens",
    indices = [Index("url")]
)
data class AuthTokenEntity(
    @PrimaryKey
    val url: String,
    val token: String,
    val expiry: Long
)