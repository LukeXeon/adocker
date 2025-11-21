package com.adocker.runner.data.local.dao

import androidx.room.*
import com.adocker.runner.data.local.entity.ContainerEntity
import com.adocker.runner.data.local.entity.ImageEntity
import com.adocker.runner.data.local.entity.LayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao {
    @Query("SELECT * FROM images ORDER BY created DESC")
    fun getAllImages(): Flow<List<ImageEntity>>

    @Query("SELECT * FROM images WHERE id = :id")
    suspend fun getImageById(id: String): ImageEntity?

    @Query("SELECT * FROM images WHERE repository = :repository AND tag = :tag")
    suspend fun getImageByRepoTag(repository: String, tag: String): ImageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: ImageEntity)

    @Delete
    suspend fun deleteImage(image: ImageEntity)

    @Query("DELETE FROM images WHERE id = :id")
    suspend fun deleteImageById(id: String)

    @Query("SELECT COUNT(*) FROM images")
    suspend fun getImageCount(): Int
}

@Dao
interface ContainerDao {
    @Query("SELECT * FROM containers ORDER BY created DESC")
    fun getAllContainers(): Flow<List<ContainerEntity>>

    @Query("SELECT * FROM containers WHERE id = :id")
    suspend fun getContainerById(id: String): ContainerEntity?

    @Query("SELECT * FROM containers WHERE name = :name")
    suspend fun getContainerByName(name: String): ContainerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContainer(container: ContainerEntity)

    @Update
    suspend fun updateContainer(container: ContainerEntity)

    @Delete
    suspend fun deleteContainer(container: ContainerEntity)

    @Query("DELETE FROM containers WHERE id = :id")
    suspend fun deleteContainerById(id: String)

    @Query("UPDATE containers SET status = :status WHERE id = :id")
    suspend fun updateContainerStatus(id: String, status: String)

    @Query("UPDATE containers SET pid = :pid, status = :status WHERE id = :id")
    suspend fun updateContainerRunning(id: String, pid: Int?, status: String)

    @Query("SELECT COUNT(*) FROM containers")
    suspend fun getContainerCount(): Int

    @Query("SELECT COUNT(*) FROM containers WHERE status = 'RUNNING'")
    suspend fun getRunningContainerCount(): Int
}

@Dao
interface LayerDao {
    @Query("SELECT * FROM layers WHERE digest = :digest")
    suspend fun getLayerByDigest(digest: String): LayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
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
