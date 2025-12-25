package com.github.andock.daemon.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.andock.daemon.database.model.ImageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao {
    @Query("SELECT * FROM images ORDER BY created DESC")
    fun getAllImages(): Flow<List<ImageEntity>>

    @Query("SELECT * FROM images WHERE id = :id")
    suspend fun getImageById(id: String): ImageEntity?

    @Query("SELECT * FROM images WHERE id = :id")
    fun getImageFlowById(id: String): Flow<ImageEntity?>

    @Query("SELECT * FROM images WHERE repository = :repository AND tag = :tag")
    suspend fun getImageByRepoTag(repository: String, tag: String): ImageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: ImageEntity)
    @Delete
    suspend fun deleteImage(image: ImageEntity)

    @Query("DELETE FROM images WHERE id = :id")
    suspend fun deleteImageById(id: String)
}