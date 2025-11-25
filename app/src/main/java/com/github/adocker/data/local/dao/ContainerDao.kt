package com.github.adocker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.github.adocker.data.local.model.ContainerEntity
import com.github.adocker.data.local.model.ContainerStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ContainerDao {
    @Query("SELECT * FROM containers ORDER BY created DESC")
    fun getAllContainers(): Flow<List<ContainerEntity>>

    @Query("SELECT * FROM containers WHERE id = :id")
    suspend fun getContainerById(id: String): ContainerEntity?

    @Query("SELECT * FROM containers WHERE name = :name")
    suspend fun getContainerByName(name: String): ContainerEntity?

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertContainer(container: ContainerEntity)

    @Update
    suspend fun updateContainer(container: ContainerEntity)

    @Delete
    suspend fun deleteContainer(container: ContainerEntity)

    @Query("DELETE FROM containers WHERE id = :id")
    suspend fun deleteContainerById(id: String)

    @Query("UPDATE containers SET status = :status WHERE id = :id")
    suspend fun updateContainerStatus(id: String, status: ContainerStatus)

    @Query("UPDATE containers SET pid = :pid, status = :status WHERE id = :id")
    suspend fun updateContainerRunning(id: String, pid: Int?, status: ContainerStatus)

    @Query("SELECT COUNT(*) FROM containers")
    suspend fun getContainerCount(): Int

    @Query("SELECT COUNT(*) FROM containers WHERE status = 'RUNNING'")
    suspend fun getRunningContainerCount(): Int
}