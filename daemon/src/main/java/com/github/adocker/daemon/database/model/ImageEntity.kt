package com.github.adocker.daemon.database.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.github.adocker.daemon.registry.model.ImageConfig
import java.util.UUID

/**
 * Room database entities
 */

@Entity(tableName = "images")
@TypeConverters(Converters::class)
data class ImageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val repository: String,
    val tag: String,
    val digest: String,
    val architecture: String,
    val os: String,
    val created: Long = System.currentTimeMillis(),
    val size: Long = 0,
    val layerIds: List<String> = emptyList(),
    val config: ImageConfig? = null
) {
    @get:Ignore
    val fullName: String
        get() = "$repository:$tag"
}