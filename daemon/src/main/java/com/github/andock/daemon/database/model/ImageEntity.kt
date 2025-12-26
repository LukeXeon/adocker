package com.github.andock.daemon.database.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.github.andock.daemon.client.model.ImageConfig

/**
 * Room database entities
 */

@Entity(
    tableName = "images",
    indices = [
        Index("id"),
    ]
)
@TypeConverters(Converters::class)
data class ImageEntity(
    @PrimaryKey
    val id: String,
    val registry: String,
    val repository: String,
    val tag: String,
    val architecture: String,
    val os: String,
    val created: Long,
    val size: Long,
    val layerIds: List<String>,
    val config: ImageConfig?
) {
    @get:Ignore
    val fullName: String
        get() = "$repository:$tag"
}