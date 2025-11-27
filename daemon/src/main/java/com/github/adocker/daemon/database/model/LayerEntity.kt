package com.github.adocker.daemon.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "layers")
data class LayerEntity(
    @PrimaryKey
    val digest: String,
    val size: Long,
    val mediaType: String,
    val downloaded: Boolean,
    val refCount: Int = 1
)