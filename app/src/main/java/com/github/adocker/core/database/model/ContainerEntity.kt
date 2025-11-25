package com.github.adocker.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.UUID

@Entity(tableName = "containers")
@TypeConverters(Converters::class)
data class ContainerEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val imageId: String,
    val imageName: String,
    val created: Long = System.currentTimeMillis(),
    val status: ContainerStatus = ContainerStatus.CREATED,
    val config: ContainerConfig = ContainerConfig(),
    val pid: Int? = null
)