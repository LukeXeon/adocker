package com.adocker.runner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room database entities
 */

@Entity(tableName = "images")
@TypeConverters(Converters::class)
data class ImageEntity(
    @PrimaryKey
    val id: String,
    val repository: String,
    val tag: String,
    val digest: String,
    val architecture: String,
    val os: String,
    val created: Long,
    val size: Long,
    val layerIds: List<String>,
    val configJson: String?
)

@Entity(tableName = "containers")
@TypeConverters(Converters::class)
data class ContainerEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val imageId: String,
    val imageName: String,
    val created: Long,
    val status: String,
    val configJson: String,
    val pid: Int?
)

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

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return json.decodeFromString(value)
    }
}
