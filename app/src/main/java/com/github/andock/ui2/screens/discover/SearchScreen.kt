package com.github.andock.ui2.screens.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.github.andock.R
import com.github.andock.ui.screens.search.SearchViewModel
import com.github.andock.ui.theme.IconSize
import com.github.andock.ui.theme.Spacing
import com.github.andock.ui2.components.SearchResultCard

/**
 * Search screen for discovering Docker Hub images.
 *
 * Features:
 * - Real-time search with debouncing (400ms)
 * - Infinite scroll pagination using Paging 3
 * - Search history
 * - Advanced filters (official images, minimum stars)
 * - Image pull progress tracking
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val showOnlyOfficial by viewModel.showOnlyOfficial.collectAsState()
    val minStars by viewModel.minStars.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()

    val searchResults = viewModel.searchResults.collectAsLazyPagingItems()
    val focusManager = LocalFocusManager.current

    var showFilters by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_discover)) },
                actions = {
                    // Filter button
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Badge(
                            containerColor = if (showOnlyOfficial || minStars > 0) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filters")
                        }
                    }

                    // History button
                    if (searchHistory.isNotEmpty()) {
                        IconButton(onClick = { showHistory = !showHistory }) {
                            Icon(Icons.Default.History, contentDescription = "Search History")
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.Medium),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text(stringResource(R.string.discover_search_placeholder)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            viewModel.performSearch(searchQuery)
                            focusManager.clearFocus()
                        }
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Filters panel
            if (showFilters) {
                FilterPanel(
                    showOnlyOfficial = showOnlyOfficial,
                    minStars = minStars,
                    onToggleOfficial = { viewModel.toggleOfficialFilter() },
                    onMinStarsChange = { viewModel.updateMinStars(it) },
                    modifier = Modifier.padding(horizontal = Spacing.Medium)
                )
                Spacer(modifier = Modifier.height(Spacing.Small))
            }

            // Search history
            if (showHistory && searchHistory.isNotEmpty()) {
                SearchHistoryPanel(
                    history = searchHistory,
                    onHistoryItemClick = {
                        viewModel.searchFromHistory(it)
                        showHistory = false
                    },
                    onRemoveItem = { viewModel.removeFromHistory(it) },
                    onClearHistory = {
                        viewModel.clearSearchHistory()
                        showHistory = false
                    },
                    modifier = Modifier.padding(horizontal = Spacing.Medium)
                )
                Spacer(modifier = Modifier.height(Spacing.Small))
            }

            // Content
            when {
                searchResults.loadState.refresh is LoadState.Loading && searchResults.itemCount == 0 -> {
                    // Initial loading
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                searchResults.loadState.refresh is LoadState.Error && searchResults.itemCount == 0 -> {
                    // Error state
                    ErrorState(
                        error = (searchResults.loadState.refresh as LoadState.Error).error,
                        onRetry = { searchResults.retry() }
                    )
                }

                searchResults.itemCount == 0 && searchQuery.isNotBlank() -> {
                    // No results
                    NoResultsState()
                }

                searchQuery.isBlank() -> {
                    // Initial/empty state
                    InitialState()
                }

                else -> {
                    // Results list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = Spacing.Medium,
                            end = Spacing.Medium,
                            bottom = Spacing.Medium
                        ),
                        verticalArrangement = Arrangement.spacedBy(Spacing.Small)
                    ) {
                        // Filter results in UI
                        val filteredIndices = (0 until searchResults.itemCount).filter { index ->
                            val result = searchResults[index]
                            result != null && run {
                                val officialMatch = !showOnlyOfficial || result.isOfficial
                                val starsMatch = result.starCount >= minStars
                                officialMatch && starsMatch
                            }
                        }

                        items(
                            count = filteredIndices.size,
                            key = { index -> searchResults[filteredIndices[index]]?.repoName ?: index }
                        ) { index ->
                            val result = searchResults[filteredIndices[index]]
                            if (result != null) {
                                val isDownloading = activeDownloads.containsKey(result.repoName)

                                SearchResultCard(
                                    result = result,
                                    onPull = {
                                        result.repoName?.let { name ->
                                            viewModel.pullImage(name)
                                        }
                                    },
                                    isPulling = isDownloading
                                )
                            }
                        }

                        // Loading indicator for pagination
                        if (searchResults.loadState.append is LoadState.Loading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Spacing.Medium),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }

                        // Error indicator for pagination
                        if (searchResults.loadState.append is LoadState.Error) {
                            item {
                                PaginationErrorItem(
                                    onRetry = { searchResults.retry() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPanel(
    showOnlyOfficial: Boolean,
    minStars: Int,
    onToggleOfficial: () -> Unit,
    onMinStarsChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            Text(
                text = "Filters",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Official images only", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = showOnlyOfficial,
                    onCheckedChange = { onToggleOfficial() }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Minimum stars: $minStars", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    FilledTonalButton(
                        onClick = { onMinStarsChange(0) },
                        enabled = minStars != 0
                    ) {
                        Text("Reset")
                    }
                    FilledTonalButton(onClick = { onMinStarsChange(10) }) { Text("10+") }
                    FilledTonalButton(onClick = { onMinStarsChange(100) }) { Text("100+") }
                    FilledTonalButton(onClick = { onMinStarsChange(1000) }) { Text("1000+") }
                }
            }
        }
    }
}

@Composable
private fun SearchHistoryPanel(
    history: List<String>,
    onHistoryItemClick: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Searches",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onClearHistory) {
                    Text("Clear All")
                }
            }

            history.take(5).forEach { query ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onHistoryItemClick(query) }
                        .padding(vertical = Spacing.Small),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(IconSize.Small),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(Spacing.Small))
                        Text(
                            text = query,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    IconButton(onClick = { onRemoveItem(query) }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            modifier = Modifier.size(IconSize.Small)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InitialState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(Spacing.Large)
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(IconSize.Huge),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(Spacing.Medium))
            Text(
                text = stringResource(R.string.images_search_hint_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.Small))
            Text(
                text = stringResource(R.string.images_search_hint_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun NoResultsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(Spacing.Large)
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(IconSize.Huge),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(Spacing.Medium))
            Text(
                text = stringResource(R.string.images_search_no_results),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.Small))
            Text(
                text = stringResource(R.string.images_search_no_results_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ErrorState(
    error: Throwable,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(Spacing.Large)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(IconSize.Huge),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(Spacing.Medium))
            Text(
                text = "Search Failed",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(Spacing.Small))
            Text(
                text = error.message ?: "Unknown error",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Spacing.Large))
            FilledTonalButton(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.Small))
                Text("Retry")
            }
        }
    }
}

@Composable
private fun PaginationErrorItem(
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.Medium),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Failed to load more results",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(Spacing.Small))
            TextButton(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}
