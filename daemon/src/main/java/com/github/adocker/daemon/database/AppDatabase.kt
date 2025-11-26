package com.github.adocker.daemon.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.github.adocker.daemon.database.dao.ContainerDao
import com.github.adocker.daemon.database.dao.ImageDao
import com.github.adocker.daemon.database.dao.LayerDao
import com.github.adocker.daemon.database.dao.MirrorDao
import com.github.adocker.daemon.database.model.ContainerEntity
import com.github.adocker.daemon.database.model.Converters
import com.github.adocker.daemon.database.model.ImageEntity
import com.github.adocker.daemon.database.model.LayerEntity
import com.github.adocker.daemon.database.model.MirrorEntity

@Database(
    entities = [
        ImageEntity::class,
        ContainerEntity::class,
        LayerEntity::class,
        MirrorEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun imageDao(): ImageDao
    abstract fun containerDao(): ContainerDao
    abstract fun layerDao(): LayerDao
    abstract fun mirrorDao(): MirrorDao
}
