package com.github.andock.daemon.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.andock.daemon.database.model.SearchRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchRecordDao {
    /**
     * Get all search records ordered by most recent first.
     */
    @Query("SELECT `query` FROM search_records ORDER BY updateAt DESC LIMIT 20")
    fun getAllAsFlow(): Flow<List<String>>

    /**
     * Insert or update a search record.
     * If the query already exists, it will update the timestamp.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: SearchRecordEntity)

    /**
     * Delete a specific search record by query.
     */
    @Query("DELETE FROM search_records WHERE `query` = :query")
    suspend fun deleteByQuery(query: String)

    /**
     * Clear all search records.
     */
    @Query("DELETE FROM search_records")
    suspend fun deleteAll()

    /**
     * Delete old search records, keeping only the most recent 20.
     */
    @Query("""
        DELETE FROM search_records
        WHERE `query` NOT IN (
            SELECT `query` FROM search_records
            ORDER BY updateAt DESC
            LIMIT 20
        )
    """)
    suspend fun trim()
}