package com.github.adocker.daemon.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "registry_servers")
@TypeConverters(Converters::class)
data class RegistryEntity(
    @PrimaryKey
    val id: String,
    val url: String,
    val name: String,
    val bearerToken: String? = null,
    val type: RegistryType,
    val priority: Int = 0,
)