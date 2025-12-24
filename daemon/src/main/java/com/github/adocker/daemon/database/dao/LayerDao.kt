package com.github.adocker.daemon.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.adocker.daemon.database.model.LayerEntity

@Dao
interface LayerDao {
    @Query("SELECT * FROM layers WHERE digest = :digest")
    suspend fun getLayerByDigest(digest: String): LayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayer(layer: LayerEntity)

    @Query(
        """
        DELETE FROM layers
        WHERE digest = :digest
        AND NOT EXISTS (
            SELECT 1 FROM layer_references WHERE layerDigest = :digest
        )
    """
    )
    suspend fun deleteUnreferencedLayer(digest: String): Int
}