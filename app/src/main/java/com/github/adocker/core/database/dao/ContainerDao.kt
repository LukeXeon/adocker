package com.github.adocker.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.github.adocker.core.database.model.ContainerEntity
import com.github.adocker.core.database.model.ContainerStatus
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

    @Query("SELECT COUNT(*) FROM containers")
    suspend fun getContainerCount(): Int
}