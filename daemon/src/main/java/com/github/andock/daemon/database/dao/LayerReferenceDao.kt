package com.github.andock.daemon.database.dao

import androidx.room.Dao
import androidx.room.Insert
import com.github.andock.daemon.database.model.LayerReferenceEntity

@Dao
interface LayerReferenceDao {
    @Insert
    suspend fun insertLayerReference(references: List<LayerReferenceEntity>)
}