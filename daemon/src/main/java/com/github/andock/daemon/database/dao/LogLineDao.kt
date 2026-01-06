package com.github.andock.daemon.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.github.andock.daemon.database.model.LogLineDTO
import com.github.andock.daemon.database.model.LogLineEntity

@Dao
interface LogLineDao {
    @Insert
    suspend fun append(line: LogLineEntity)

    @Query("DELETE FROM log_lines WHERE containerId = :containerId")
    suspend fun clearLogById(containerId: String)

    @Query("SELECT id, timestamp, isError, message FROM log_lines WHERE containerId = :containerId ORDER BY timestamp ASC LIMIT :pageSize OFFSET :offset")
    suspend fun getLogLines(containerId: String, offset: Long, pageSize: Long): List<LogLineDTO>

    @Query("SELECT COUNT(*) FROM log_lines WHERE containerId = :containerId")
    suspend fun getTotalCount(containerId: String): Int

    @Query("DELETE FROM log_lines")
    suspend fun clearAll()
}