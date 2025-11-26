package com.github.adocker.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "registry_mirrors")
data class MirrorEntity(
    @PrimaryKey
    val url: String,
    val name: String,
    val bearerToken: String? = null,
    val isBuiltIn: Boolean = true,
    val priority: Int = 0,
    val isHealthy: Boolean = true,
    val latencyMs: Long = -1,
    val lastChecked: Long = 0
)