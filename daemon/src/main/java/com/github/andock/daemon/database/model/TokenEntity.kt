package com.github.andock.daemon.database.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    "tokens",
    indices = [Index("url")]
)
data class TokenEntity(
    @PrimaryKey
    val url: String,
    val token: String,
    val expiry: Long
)