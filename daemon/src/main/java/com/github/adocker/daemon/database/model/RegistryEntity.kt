package com.github.adocker.daemon.database.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(
    tableName = "registries",
    indices = [
        Index("id"),
    ]
)
@TypeConverters(Converters::class)
data class RegistryEntity(
    @PrimaryKey
    val id: String,
    val url: String,
    val name: String,
    val bearerToken: String? = null,
    val type: RegistryType,
    val priority: Int = 0,
) : Comparable<RegistryEntity> {
    override fun compareTo(other: RegistryEntity): Int {
        var c = this.type.compareTo(other.type)
        if (c != 0) {
            return c
        }
        c = other.priority.compareTo(this.priority)
        if (c != 0) {
            return c
        }
        c = this.id.compareTo(other.id)
        if (c != 0) {
            return c
        }
        c = this.name.compareTo(other.name)
        if (c != 0) {
            return c
        }
        c = this.url.compareTo(other.url)
        if (c != 0) {
            return c
        }
        c = (this.bearerToken ?: "").compareTo((other.bearerToken ?: ""))
        if (c != 0) {
            return c
        }
        return 0
    }
}