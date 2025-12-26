package com.github.andock.daemon.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.github.andock.daemon.database.dao.ContainerDao
import com.github.andock.daemon.database.dao.ImageDao
import com.github.andock.daemon.database.dao.LayerDao
import com.github.andock.daemon.database.dao.RegistryDao
import com.github.andock.daemon.database.dao.TokenDao
import com.github.andock.daemon.database.model.ContainerEntity
import com.github.andock.daemon.database.model.Converters
import com.github.andock.daemon.database.model.ImageEntity
import com.github.andock.daemon.database.model.LayerEntity
import com.github.andock.daemon.database.model.LayerReference
import com.github.andock.daemon.database.model.RegistryEntity
import com.github.andock.daemon.database.model.TokenEntity

@Database(
    entities = [
        ImageEntity::class,
        ContainerEntity::class,
        LayerEntity::class,
        LayerReference::class,
        RegistryEntity::class,
        TokenEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun imageDao(): ImageDao
    abstract fun containerDao(): ContainerDao
    abstract fun layerDao(): LayerDao
    abstract fun registryDao(): RegistryDao

    abstract fun tokenDao(): TokenDao
}
