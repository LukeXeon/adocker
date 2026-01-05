package com.github.andock.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.github.andock.R
import com.github.andock.ui.components.PaginationColumn
import com.github.andock.ui.components.PaginationEmptyPlaceholder
import com.github.andock.ui.components.PaginationErrorPlaceholder
import com.github.andock.ui.screens.images.ImageTagSelectRoute
import com.github.andock.ui.screens.main.LocalNavController
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
    val navController = LocalNavController.current
    val searchQuery by viewModel.query.collectAsState()
    val isOfficialOnly by viewModel.isOfficialOnly.collectAsState()
    val searchHistory by viewModel.history.collectAsState()
    val searchResults = viewModel.results.collectAsLazyPagingItems()
    val focusManager = LocalFocusManager.current
    val (showFilters, setShowFilters) = remember { mutableStateOf(false) }
    val (showHistory, setShowHistory) = remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_discover)) },
                actions = {
                    // Filter button
                    IconButton(onClick = { setShowFilters(!showFilters) }) {
                        Badge(
                            containerColor = if (isOfficialOnly) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Transparent
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
                    onValueChange = { viewModel.setQuery(it) },
                    placeholder = { Text(stringResource(R.string.discover_search_placeholder)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.setQuery("") }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear"
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            viewModel.setQuery(searchQuery)
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
                    showOnlyOfficial = isOfficialOnly,
                    onToggleOfficial = {
                        viewModel.setOfficialOnly(!isOfficialOnly)
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
                        viewModel.setQuery(it)
                        setShowHistory(false)
                    },
                    onRemoveItem = { viewModel.removeHistory(it) },
                    onClearHistory = {
                        viewModel.clearHistory()
                        setShowHistory(false)
                    },
                    modifier = Modifier.padding(horizontal = Spacing.Medium)
                )
                Spacer(modifier = Modifier.height(Spacing.Small))
            }

            // Content
            if (searchQuery.isBlank()) {
                SearchInitialState()
            } else {
                PaginationColumn(
                    searchResults,
                    PaginationEmptyPlaceholder(
                        Icons.Default.Search,
                        stringResource(R.string.images_search_no_results),
                        stringResource(R.string.images_search_no_results_desc)
                    ),
                    PaginationErrorPlaceholder("Search Failed"),
                    { it.repoName ?: "" }) { result ->
                    SearchResultCard(
                        result = result,
                        onPull = {
                            result.repoName?.let { name ->
                                navController.navigate(
                                    ImageTagSelectRoute(name)
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}
