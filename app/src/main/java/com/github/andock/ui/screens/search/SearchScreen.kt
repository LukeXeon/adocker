package com.github.andock.ui.screens.search

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.github.andock.R
import com.github.andock.daemon.images.downloader.ImageDownloader
import com.github.andock.ui.components.PaginationErrorItem
import com.github.andock.ui.screens.images.ImageDownloadDialog
import com.github.andock.ui.theme.Spacing

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
fun SearchScreen() {
    val viewModel = hiltViewModel<SearchViewModel>()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val searchResults = viewModel.searchResults.collectAsLazyPagingItems()
    val focusManager = LocalFocusManager.current
    val (showProgressDialog, setProgressDialog) = remember { mutableStateOf<ImageDownloader?>(null) }
    val (showOnlyOfficial, setShowOnlyOfficial) = remember { mutableStateOf(false) }
    val (minStars, setMinStars) = remember { mutableStateOf(0) }
    val (showFilters, setShowFilters) = remember { mutableStateOf(false) }
    val (showHistory, setShowHistory) = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_discover)) },
                actions = {
                    // Filter button
                    IconButton(onClick = { setShowFilters(!showFilters) }) {
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
                        IconButton(onClick = { setShowHistory(!showHistory) }) {
                            Icon(Icons.Default.History, contentDescription = "Search History")
                        }
                    }
                }
            )
        },
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
                SearchFilterPanel(
                    showOnlyOfficial = showOnlyOfficial,
                    minStars = minStars,
                    onToggleOfficial = {
                        setShowOnlyOfficial(!showOnlyOfficial)
                    },
                    onMinStarsChange = {
                        setMinStars(it)
                    },
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
                        setShowHistory(false)
                    },
                    onRemoveItem = { viewModel.removeFromHistory(it) },
                    onClearHistory = {
                        viewModel.clearSearchHistory()
                        setShowHistory(false)
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
                    SearchErrorState(
                        error = (searchResults.loadState.refresh as LoadState.Error).error,
                        onRetry = { searchResults.retry() }
                    )
                }

                searchResults.itemCount == 0 && searchQuery.isNotBlank() -> {
                    // No results
                    SearchNoResultsState()
                }

                searchQuery.isBlank() -> {
                    // Initial/empty state
                    SearchInitialState()
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
                            key = { index -> filteredIndices[index] }
                        ) { index ->
                            val result = searchResults[filteredIndices[index]]
                            if (result != null) {
                                SearchResultCard(
                                    result = result,
                                    onPull = {
                                        result.repoName?.let { name ->
                                            setProgressDialog(viewModel.pullImage(name))
                                        }
                                    },
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
    if (showProgressDialog != null) {
        ImageDownloadDialog(
            downloader = showProgressDialog,
            onDismissRequest = {
                setProgressDialog(null)
            }
        )
    }
}
