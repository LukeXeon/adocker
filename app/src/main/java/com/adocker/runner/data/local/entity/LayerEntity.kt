package com.adocker.runner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "layers")
data class LayerEntity(
    @PrimaryKey
    val digest: String,
    val size: Long,
    val mediaType: String,
    val downloaded: Boolean,
    val extracted: Boolean,
    val refCount: Int = 1
)