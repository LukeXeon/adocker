package com.github.adocker.daemon.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "registry_mirrors")
data class MirrorEntity(
    @PrimaryKey
    val id: String,
    val url: String,
    val name: String,
    val bearerToken: String? = null,
    val isBuiltIn: Boolean = true,
    val priority: Int = 0,
)