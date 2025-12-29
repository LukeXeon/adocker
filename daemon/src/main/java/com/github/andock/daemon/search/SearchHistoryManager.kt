package com.github.andock.daemon.search

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.searchHistoryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "search_history"
)

/**
 * Manages search history using DataStore Preferences.
 *
 * Stores up to [MAX_HISTORY_SIZE] most recent search queries in reverse chronological order.
 * Uses a `Set<String>` internally but exposes a sorted `List<String>` via Flow.
 *
 * ## Storage Details
 * - **DataStore Name**: `search_history`
 * - **Key**: `stringSetPreferencesKey("search_history")`
 * - **Max Size**: 20 items
 * - **Order**: Most recent first
 *
 * ## Thread Safety
 * All write operations use `DataStore.edit {}` which is thread-safe and suspending.
 *
 * ## Usage
 * ```kotlin
 * @HiltViewModel
 * class SearchViewModel @Inject constructor(
 *     private val searchHistoryManager: SearchHistoryManager
 * ) : ViewModel() {
 *     val searchHistory = searchHistoryManager.searchHistory
 *         .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
 *
 *     fun search(query: String) {
 *         viewModelScope.launch {
 *             searchHistoryManager.addToHistory(query)
 *         }
 *     }
 * }
 * ```
 *
 * @param context Application context (injected via [ApplicationContext])
 */
@Singleton
class SearchHistoryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.searchHistoryDataStore

    companion object {
        private val SEARCH_HISTORY_KEY = stringSetPreferencesKey("search_history")
        private const val MAX_HISTORY_SIZE = 20
    }

    /**
     * Flow of search history (most recent first).
     *
     * Emits a new list whenever the history changes. The list is ordered
     * with the most recent search query at index 0.
     */
    val searchHistory: Flow<List<String>> = dataStore.data.map { preferences ->
        preferences[SEARCH_HISTORY_KEY]?.toList() ?: emptyList()
    }

    /**
     * Add a search query to history.
     *
     * If the query already exists, it will be moved to the top (most recent).
     * Blank queries are ignored.
     *
     * @param query The search query to add
     */
    suspend fun addToHistory(query: String) {
        if (query.isBlank()) return

        dataStore.edit { preferences ->
            val currentHistory = preferences[SEARCH_HISTORY_KEY]?.toMutableList() ?: mutableListOf()

            // Remove if already exists (to move to top)
            currentHistory.remove(query)

            // Add to beginning
            currentHistory.add(0, query)

            // Keep only MAX_HISTORY_SIZE items
            val trimmedHistory = currentHistory.take(MAX_HISTORY_SIZE)

            preferences[SEARCH_HISTORY_KEY] = trimmedHistory.toSet()
        }
    }

    /**
     * Clear all search history.
     *
     * Removes all stored search queries from DataStore.
     */
    suspend fun clearHistory() {
        dataStore.edit { preferences ->
            preferences.remove(SEARCH_HISTORY_KEY)
        }
    }

    /**
     * Remove a specific query from history.
     *
     * If the query doesn't exist in history, this is a no-op.
     *
     * @param query The query to remove
     */
    suspend fun removeFromHistory(query: String) {
        dataStore.edit { preferences ->
            val currentHistory = preferences[SEARCH_HISTORY_KEY]?.toMutableList() ?: return@edit
            currentHistory.remove(query)
            preferences[SEARCH_HISTORY_KEY] = currentHistory.toSet()
        }
    }
}
