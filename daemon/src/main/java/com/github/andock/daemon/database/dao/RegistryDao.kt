package com.github.andock.daemon.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.andock.daemon.database.model.RegistryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistryDao {
    @Query("SELECT id FROM registries")
    suspend fun getAllIds(): List<String>

    @Query("SELECT * FROM registries WHERE id = :id")
    suspend fun findById(id: String): RegistryEntity?

    @Query("SELECT COUNT(*) FROM registries")
    fun getCountAsFlow(): Flow<Int>

    @Query("SELECT * FROM registries WHERE id = :id")
    fun getByIdAsFlow(id: String): Flow<RegistryEntity?>

    @Query("SELECT bearerToken FROM registries WHERE url = :url")
    suspend fun getTokenByUrl(url: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mirror: RegistryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mirrors: List<RegistryEntity>)

    @Query("DELETE FROM registries WHERE id = :id")
    suspend fun deleteById(id: String)
}