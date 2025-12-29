package com.github.andock.daemon.search

import com.github.andock.daemon.database.dao.SearchRecordDao
import com.github.andock.daemon.database.model.SearchRecordEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages search history using Room database.
 *
 * Stores up to 20 most recent search queries in reverse chronological order.
 *
 * ## Storage Details
 * - **Database Table**: `search_records`
 * - **Max Size**: 20 items
 * - **Order**: Most recent first (by `updateAt` timestamp)
 *
 * ## Thread Safety
 * All write operations use Room's suspending functions which are thread-safe.
 *
 * ## Usage
 * ```kotlin
 * @HiltViewModel
 * class SearchViewModel @Inject constructor(
 *     private val searchHistory: SearchHistory
 * ) : ViewModel() {
 *     val searchHistory = searchHistory.searchRecords
 *         .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
 *
 *     fun search(query: String) {
 *         viewModelScope.launch {
 *             searchHistory.addToHistory(query)
 *         }
 *     }
 * }
 * ```
 */
@Singleton
class SearchHistory @Inject constructor(
    private val searchRecordDao: SearchRecordDao,
) {
    /**
     * Flow of search history (most recent first).
     *
     * Emits a new list whenever the history changes. The list is ordered
     * with the most recent search query at index 0.
     * Limited to the most recent 20 items.
     */
    val searchRecords: Flow<List<String>> = searchRecordDao.getSearchRecords()

    /**
     * Add a search query to history.
     *
     * If the query already exists, it will be moved to the top (most recent) by updating its timestamp.
     * Blank queries are ignored.
     *
     * @param query The search query to add
     */
    suspend fun addToHistory(query: String) {
        if (query.isBlank()) return

        val record = SearchRecordEntity(
            query = query.trim(),
            updateAt = System.currentTimeMillis()
        )

        // Insert or replace (updates timestamp if already exists)
        searchRecordDao.insertSearchRecord(record)

        // Trim old records to keep only the most recent MAX_HISTORY_SIZE
        searchRecordDao.trimOldRecords()
    }

    /**
     * Clear all search history.
     *
     * Removes all stored search queries from the database.
     */
    suspend fun clearHistory() {
        searchRecordDao.clearAllSearchRecords()
    }

    /**
     * Remove a specific query from history.
     *
     * If the query doesn't exist in history, this is a no-op.
     *
     * @param query The query to remove
     */
    suspend fun removeFromHistory(query: String) {
        searchRecordDao.deleteSearchRecord(query)
    }
}
