package com.github.andock.daemon.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.github.andock.daemon.database.model.ContainerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContainerDao {
    @Query("SELECT * FROM containers")
    suspend fun getAllContainers(): List<ContainerEntity>

    @Query("SELECT id FROM containers")
    suspend fun getAllContainerIds(): List<String>

    @Query("SELECT * FROM containers WHERE id = :id")
    suspend fun getContainerById(id: String): ContainerEntity?

    @Query("SELECT * FROM containers WHERE id = :id")
    fun getContainerFlowById(id: String): Flow<ContainerEntity?>

    @Query("SELECT * FROM containers WHERE name = :name")
    suspend fun getContainerByName(name: String): ContainerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContainer(container: ContainerEntity)

    @Update
    suspend fun updateContainer(container: ContainerEntity)

    @Query("DELETE FROM containers WHERE id = :id")
    suspend fun deleteContainerById(id: String)

    @Query("SELECT COUNT(*) FROM containers")
    suspend fun getContainerCount(): Int

    @Query("UPDATE containers SET lastRunAt = :timestamp WHERE id = :id")
    suspend fun setContainerLastRun(id: String, timestamp: Long)
}