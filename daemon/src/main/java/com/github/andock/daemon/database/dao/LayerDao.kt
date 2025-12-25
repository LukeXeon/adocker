package com.github.andock.daemon.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.andock.daemon.database.model.LayerEntity

@Dao
interface LayerDao {
    @Query("SELECT * FROM layers WHERE id = :id")
    suspend fun getLayerById(id: String): LayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayer(layer: LayerEntity)

    @Query(
        """
        DELETE FROM layers
        WHERE id = :id
        AND NOT EXISTS (
            SELECT 1 FROM layer_references WHERE layerId = :id
        )
    """
    )
    suspend fun deleteUnreferencedLayer(id: String): Int
}