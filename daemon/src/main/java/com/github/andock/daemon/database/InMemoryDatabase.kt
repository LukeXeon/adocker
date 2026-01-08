package com.github.andock.daemon.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.github.andock.daemon.database.dao.InMemoryLogDao
import com.github.andock.daemon.database.model.InMemoryLogEntity

@Database(
    entities = [
        InMemoryLogEntity::class,
    ],
    version = 1,
    exportSchema = false
)
abstract class InMemoryDatabase : RoomDatabase() {
    abstract val logDao: InMemoryLogDao
}