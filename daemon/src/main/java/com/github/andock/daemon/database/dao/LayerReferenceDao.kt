package com.github.andock.daemon.database.dao

import androidx.room.Dao
import androidx.room.Insert
import com.github.andock.daemon.database.model.LayerReferenceEntity

@Dao
interface LayerReferenceDao {
    @Insert
    suspend fun insertLayerReferences(references: List<LayerReferenceEntity>)
}