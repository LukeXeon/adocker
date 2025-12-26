package com.github.andock.daemon.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.github.andock.daemon.database.model.LayerEntity

@Dao
interface LayerDao {
    @Query("SELECT * FROM layers WHERE id = :id")
    suspend fun getLayerById(id: String): LayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayer(layer: LayerEntity)

    // 步骤1：查询所有未被引用的layer id（待删除的ID）
    @Query(
        """
        SELECT id FROM layers
        WHERE NOT EXISTS (
            SELECT 1 FROM layer_references WHERE layerId = layers.id
        )
        """
    )
    suspend fun getUnreferencedLayerIds(): List<String> // 假设id是Long类型，若为String则改String

    // 步骤2：根据ID列表删除layer（批量删除）
    @Query("DELETE FROM layers WHERE id IN (:ids)")
    suspend fun deleteLayersByIds(ids: List<String>): Int // 返回删除的行数

    // 步骤3：事务封装：先查ID → 再删除 → 返回ID列表
    @Transaction // 事务保证：查询和删除要么都成功，要么都失败
    suspend fun deleteUnreferencedLayers(): List<String> {
        // 1. 查询所有待删除的ID
        val toDeleteIds = getUnreferencedLayerIds()
        // 2. 若有ID则删除，无则直接返回空列表
        if (toDeleteIds.isNotEmpty()) {
            deleteLayersByIds(toDeleteIds)
        }
        // 3. 返回被删除的ID列表
        return toDeleteIds
    }
}