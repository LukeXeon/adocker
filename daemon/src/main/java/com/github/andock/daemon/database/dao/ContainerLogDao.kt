package com.github.andock.daemon.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.github.andock.daemon.database.model.ContainerLogEntity
import com.github.andock.daemon.database.model.LogLine

@Dao
interface ContainerLogDao {
    @Insert
    suspend fun append(line: ContainerLogEntity)

    @Query("DELETE FROM log_lines WHERE containerId = :containerId")
    suspend fun deleteById(containerId: String)

    @Query(
        """
      SELECT
      id,
      datetime(timestamp / 1000, 'unixepoch', 'localtime') || ' ' || message AS content
      FROM log_lines 
      WHERE containerId = :containerId 
      ORDER BY timestamp ASC
      """
    )
    fun getAllAsPaging(containerId: String): PagingSource<Int, LogLine>

}