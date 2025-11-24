package com.adocker.runner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

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