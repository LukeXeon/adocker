package com.adocker.runner.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.adocker.runner.data.local.dao.ContainerDao
import com.adocker.runner.data.local.dao.ImageDao
import com.adocker.runner.data.local.dao.LayerDao
import com.adocker.runner.data.local.entity.ContainerEntity
import com.adocker.runner.data.local.entity.Converters
import com.adocker.runner.data.local.entity.ImageEntity
import com.adocker.runner.data.local.entity.LayerEntity

@Database(
    entities = [ImageEntity::class, ContainerEntity::class, LayerEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun imageDao(): ImageDao
    abstract fun containerDao(): ContainerDao
    abstract fun layerDao(): LayerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "adocker_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
