package com.github.andock.daemon.database.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "layers",
    indices = [
        Index("id")
    ]
)
data class LayerEntity(
    /**
     * digest
     * */
    @PrimaryKey
    val id: String,
    val size: Long,
    val mediaType: String,
)