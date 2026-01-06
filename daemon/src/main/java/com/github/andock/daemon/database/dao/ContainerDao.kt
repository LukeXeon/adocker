package com.github.andock.daemon.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.andock.daemon.database.model.ContainerDTO
import com.github.andock.daemon.database.model.ContainerEntity
import com.github.andock.daemon.images.models.ContainerConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface ContainerDao {
    @Query("SELECT id,lastRunAt FROM containers")
    suspend fun getAllContainers(): List<ContainerDTO>

    @Query("SELECT id FROM containers")
    suspend fun getAllContainerIds(): List<String>

    @Query("SELECT config FROM containers WHERE id = :id")
    suspend fun getContainerConfigById(id: String): ContainerConfig?

    @Query("SELECT * FROM containers WHERE id = :id")
    fun getContainerFlowById(id: String): Flow<ContainerEntity?>

    @Query("SELECT COUNT(*) FROM containers WHERE name = :name")
    suspend fun hasContainerByName(name: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContainer(container: ContainerEntity)

    @Query("DELETE FROM containers WHERE id = :id")
    suspend fun deleteContainerById(id: String)

    @Query("UPDATE containers SET lastRunAt = :timestamp WHERE id = :id")
    suspend fun setContainerLastRun(id: String, timestamp: Long)
}