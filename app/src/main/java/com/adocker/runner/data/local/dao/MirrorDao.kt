package com.adocker.runner.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.adocker.runner.data.local.model.MirrorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MirrorDao {
    @Query("SELECT * FROM registry_mirrors ORDER BY isDefault DESC, isBuiltIn DESC, name ASC")
    fun getAllMirrors(): Flow<List<MirrorEntity>>

    @Query("SELECT * FROM registry_mirrors WHERE url = :url")
    suspend fun getMirrorByUrl(url: String): MirrorEntity?

    @Query("SELECT * FROM registry_mirrors WHERE isSelected = 1 LIMIT 1")
    suspend fun getSelectedMirror(): MirrorEntity?

    @Query("SELECT * FROM registry_mirrors WHERE isSelected = 1 LIMIT 1")
    fun getSelectedMirrorFlow(): Flow<MirrorEntity?>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertMirror(mirror: MirrorEntity)

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertMirrors(mirrors: List<MirrorEntity>)

    @Delete
    suspend fun deleteMirror(mirror: MirrorEntity)

    @Query("DELETE FROM registry_mirrors WHERE url = :url")
    suspend fun deleteMirrorByUrl(url: String)

    @Query("UPDATE registry_mirrors SET isSelected = 0")
    suspend fun clearAllSelected()

    @Query("UPDATE registry_mirrors SET isSelected = 1 WHERE url = :url")
    suspend fun selectMirrorByUrl(url: String)

    @Query("DELETE FROM registry_mirrors WHERE isBuiltIn = 0")
    suspend fun deleteAllCustomMirrors()
}