package com.github.adocker.daemon.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.adocker.daemon.database.model.MirrorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MirrorDao {
    @Query("SELECT * FROM registry_mirrors ORDER BY priority DESC, isBuiltIn DESC, name ASC")
    fun getAllMirrors(): Flow<List<MirrorEntity>>

    @Query("SELECT * FROM registry_mirrors WHERE url = :url")
    suspend fun getMirrorByUrl(url: String): MirrorEntity?

    @Query("SELECT * FROM registry_mirrors WHERE isHealthy = 1 ORDER BY latencyMs ASC, priority DESC")
    suspend fun getHealthyMirrors(): List<MirrorEntity>

    @Query("SELECT * FROM registry_mirrors WHERE isHealthy = 1 ORDER BY latencyMs ASC, priority DESC LIMIT 1")
    suspend fun getBestMirror(): MirrorEntity?

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertMirror(mirror: MirrorEntity)

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertMirrors(mirrors: List<MirrorEntity>)

    @Delete
    suspend fun deleteMirror(mirror: MirrorEntity)

    @Query("DELETE FROM registry_mirrors WHERE url = :url")
    suspend fun deleteMirrorByUrl(url: String)

    @Query("UPDATE registry_mirrors SET isHealthy = :isHealthy, latencyMs = :latencyMs, lastChecked = :lastChecked WHERE url = :url")
    suspend fun updateMirrorHealth(url: String, isHealthy: Boolean, latencyMs: Long, lastChecked: Long)

    @Query("DELETE FROM registry_mirrors WHERE isBuiltIn = 0")
    suspend fun deleteAllCustomMirrors()
}