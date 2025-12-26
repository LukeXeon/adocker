package com.github.andock.daemon.database

import androidx.room.TypeConverter
import com.github.andock.daemon.app.AppContext
import com.github.andock.daemon.client.model.ContainerConfig
import com.github.andock.daemon.client.model.ImageConfig
import com.github.andock.daemon.database.model.RegistryType
import dagger.hilt.android.EntryPointAccessors

class Converters {
    /**
     * Get Json instance from Hilt dependency graph
     * Using EntryPoint pattern since Room TypeConverters cannot use constructor injection
     */
    private val json = EntryPointAccessors.fromApplication(
        AppContext.application,
        DatabaseEntryPoint::class.java
    ).json()

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return json.decodeFromString(value)
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

    @TypeConverter
    fun fromRegistryType(value: RegistryType): Int {
        return value.ordinal
    }

    @TypeConverter
    fun toRegistryType(value: Int): RegistryType {
        return RegistryType.entries[value]
    }
}