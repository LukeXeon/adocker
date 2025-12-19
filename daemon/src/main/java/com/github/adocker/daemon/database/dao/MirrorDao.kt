package com.github.adocker.daemon.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.adocker.daemon.database.model.MirrorEntity

@Dao
interface MirrorDao {
    @Query("SELECT id FROM registry_mirrors")
    suspend fun getAllMirrorIds(): List<String>

//    @Query("SELECT * FROM registry_mirrors WHERE url = :url")
//    suspend fun getMirrorByUrl(url: String): MirrorEntity?

    @Query("SELECT * FROM registry_mirrors WHERE id = :id")
    suspend fun getMirrorById(id: String): MirrorEntity?

//    @Query("SELECT * FROM registry_mirrors WHERE isHealthy = 1 ORDER BY latencyMs ASC, priority DESC")
//    suspend fun getHealthyMirrors(): List<MirrorEntity>

//    @Query("SELECT * FROM registry_mirrors WHERE isHealthy = 1 ORDER BY latencyMs ASC, priority DESC LIMIT 1")
//    suspend fun getBestMirror(): MirrorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMirror(mirror: MirrorEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMirrors(mirrors: List<MirrorEntity>)

    @Query("DELETE FROM registry_mirrors WHERE id = :id")
    suspend fun deleteMirrorById(id: String)
}