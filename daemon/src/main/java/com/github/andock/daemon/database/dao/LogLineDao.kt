package com.github.andock.daemon.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.github.andock.daemon.database.model.LogLineEntity

@Dao
interface LogLineDao {
    @Insert
    suspend fun append(line: LogLineEntity)
    @Query("DELETE FROM log_lines WHERE containerId = :containerId")
    suspend fun clearLogById(containerId: String)

    @Query("DELETE FROM log_lines")
    suspend fun clearAll()
}