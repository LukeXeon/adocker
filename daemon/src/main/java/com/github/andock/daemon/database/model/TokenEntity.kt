package com.github.andock.daemon.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("tokens")
data class TokenEntity(
    @PrimaryKey
    val url: String,
    val token: String,
    val expiry: Long
)