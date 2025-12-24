package com.github.adocker.daemon.database.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "layers",
    indices = [
        Index("digest")
    ]
)
data class LayerEntity(
    @PrimaryKey
    val digest: String,
    val size: Long,
    val mediaType: String,
    val downloaded: Boolean,
)