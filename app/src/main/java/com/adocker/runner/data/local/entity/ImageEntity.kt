package com.adocker.runner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

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