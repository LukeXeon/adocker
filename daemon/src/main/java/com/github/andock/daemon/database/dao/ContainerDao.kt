package com.github.andock.daemon.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.andock.daemon.database.model.ContainerEntity
import com.github.andock.daemon.database.model.ContainerLastRun
import com.github.andock.daemon.images.models.ContainerConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface ContainerDao {
    @Query("SELECT id,lastRunAt FROM containers")
    suspend fun getAllLastRun(): List<ContainerLastRun>

    @Query("SELECT id FROM containers")
    suspend fun getAllIds(): List<String>

    @Query("SELECT config FROM containers WHERE id = :id")
    suspend fun findConfigById(id: String): ContainerConfig?

    @Query("SELECT * FROM containers WHERE id = :id")
    fun findByIdAsFlow(id: String): Flow<ContainerEntity?>

    @Query("SELECT COUNT(*) FROM containers WHERE name = :name")
    suspend fun hasName(name: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(container: ContainerEntity)

    @Query("DELETE FROM containers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE containers SET lastRunAt = :timestamp WHERE id = :id")
    suspend fun setLastRun(id: String, timestamp: Long)
}