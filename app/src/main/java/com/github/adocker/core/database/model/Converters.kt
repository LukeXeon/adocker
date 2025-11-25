package com.github.adocker.core.database.model

import androidx.room.TypeConverter

class Converters {
    /**
     * Get Json instance from Hilt dependency graph
     * Using EntryPoint pattern since Room TypeConverters cannot use constructor injection
     */
    private val json = ConvertersEntryPoint.instance.json()

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