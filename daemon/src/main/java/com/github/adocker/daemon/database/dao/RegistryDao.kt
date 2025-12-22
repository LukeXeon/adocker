package com.github.adocker.daemon.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.adocker.daemon.database.model.RegistryEntity

@Dao
interface RegistryDao {
    @Query("SELECT id FROM registry_servers")
    suspend fun getAllRegistryIds(): List<String>

    @Query("SELECT * FROM registry_servers WHERE id = :id")
    suspend fun getRegistryById(id: String): RegistryEntity?

    @Query("SELECT * FROM registry_servers WHERE id = :id")
    suspend fun getRegistryByUrl(id: String): RegistryEntity?

    @Query("SELECT bearerToken FROM registry_servers WHERE url = :url")
    suspend fun getBearerTokenByUrl(url: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegistry(mirror: RegistryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegistries(mirrors: List<RegistryEntity>)

    @Query("DELETE FROM registry_servers WHERE id = :id")
    suspend fun deleteRegistryById(id: String)
}