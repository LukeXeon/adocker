package com.github.andock.daemon.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.github.andock.daemon.database.model.ContainerLogEntity
import com.github.andock.daemon.database.model.ContainerLogDTO

@Dao
interface ContainerLogDao {
    @Insert
    suspend fun append(line: ContainerLogEntity)

    @Query("DELETE FROM log_lines WHERE containerId = :containerId")
    suspend fun clearLogById(containerId: String)

    @Query("""
      SELECT id, timestamp, isError, message 
      FROM log_lines 
      WHERE containerId = :containerId 
      ORDER BY timestamp ASC
      """)
    fun getLogLinesPaged(containerId: String): PagingSource<Int, ContainerLogDTO>


}