package com.github.andock.daemon.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.github.andock.daemon.database.model.InMemoryLogEntity
import com.github.andock.daemon.database.model.LogLineDTO

@Dao
interface InMemoryLogDao {
    @Insert
    suspend fun append(line: InMemoryLogEntity)

    @Query("DELETE FROM in_memory_log_lines WHERE sessionId = :sessionId")
    suspend fun clearLogById(sessionId: String)

    @Query(
        """
      SELECT id, timestamp, isError, message 
      FROM in_memory_log_lines 
      WHERE sessionId = :sessionId 
      ORDER BY timestamp ASC
      """
    )
    fun getLogLinesPaged(sessionId: String): PagingSource<Int, LogLineDTO>
}