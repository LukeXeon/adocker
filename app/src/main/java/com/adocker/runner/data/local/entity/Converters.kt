package com.adocker.runner.data.local.entity

import androidx.room.TypeConverter
import com.adocker.runner.domain.model.ContainerConfig
import com.adocker.runner.domain.model.ContainerStatus
import com.adocker.runner.domain.model.ImageConfig
import kotlinx.serialization.json.Json

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

    @TypeConverter
    fun fromContainerStatus(value: ContainerStatus): String {
        return value.name
    }

    @TypeConverter
    fun toContainerStatus(value: String): ContainerStatus {
        return ContainerStatus.valueOf(value)
    }

    @TypeConverter
    fun fromContainerConfig(value: ContainerConfig): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toContainerConfig(value: String): ContainerConfig {
        return json.decodeFromString(value)
    }

    @TypeConverter
    fun fromImageConfig(value: ImageConfig?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toImageConfig(value: String?): ImageConfig? {
        return value?.let { json.decodeFromString(it) }
    }
}