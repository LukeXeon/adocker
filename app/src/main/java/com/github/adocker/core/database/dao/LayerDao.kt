package com.github.adocker.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.adocker.core.database.model.LayerEntity

@Dao
interface LayerDao {
    @Query("SELECT * FROM layers WHERE digest = :digest")
    suspend fun getLayerByDigest(digest: String): LayerEntity?

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertLayer(layer: LayerEntity)

    @Query("UPDATE layers SET downloaded = :downloaded WHERE digest = :digest")
    suspend fun updateLayerDownloaded(digest: String, downloaded: Boolean)

    @Query("UPDATE layers SET extracted = :extracted WHERE digest = :digest")
    suspend fun updateLayerExtracted(digest: String, extracted: Boolean)

    @Query("UPDATE layers SET refCount = refCount + 1 WHERE digest = :digest")
    suspend fun incrementRefCount(digest: String)

    @Query("UPDATE layers SET refCount = refCount - 1 WHERE digest = :digest")
    suspend fun decrementRefCount(digest: String)

    @Query("DELETE FROM layers WHERE digest = :digest AND refCount <= 0")
    suspend fun deleteUnreferencedLayer(digest: String)

    @Query("SELECT * FROM layers WHERE refCount <= 0")
    suspend fun getUnreferencedLayers(): List<LayerEntity>
}