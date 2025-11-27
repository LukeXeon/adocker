package com.github.adocker.ui.screens.discover

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.adocker.ui.theme.Spacing
import com.github.adocker.ui.theme.IconSize
import com.github.adocker.R
import com.github.adocker.ui.components.SearchResultCard
import com.github.adocker.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isPulling by viewModel.isPulling.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var hasSearched by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(1) }
    val pageSize = 25

    val focusManager = LocalFocusManager.current

    fun performSearch() {
        if (searchQuery.isNotBlank()) {
            focusManager.clearFocus()
            hasSearched = true
            currentPage = 1
            viewModel.searchImages(searchQuery)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.nav_discover)) }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(Spacing.Small))

            // Search bar with button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.discover_search_placeholder)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { performSearch() }),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = { performSearch() },
                    enabled = searchQuery.isNotBlank() && !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text(stringResource(R.string.action_search))
                }
            }

            Spacer(modifier = Modifier.height(Spacing.Medium))

            // Content
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                searchResults.isNotEmpty() -> {
                    // Calculate paginated results
                    val totalPages = (searchResults.size + pageSize - 1) / pageSize
                    val startIndex = (currentPage - 1) * pageSize
                    val endIndex = minOf(startIndex + pageSize, searchResults.size)
                    val paginatedResults = searchResults.subList(startIndex, endIndex)

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = Spacing.Medium),  // 呼吸空间
                        verticalArrangement = Arrangement.spacedBy(Spacing.Small)
                    ) {
                        items(paginatedResults, key = { it.repoName ?: it.hashCode() }) { result ->
                            SearchResultCard(
                                result = result,
                                onPull = {
                                    result.repoName?.let { name ->
                                        viewModel.pullImage(name)
                                    }
                                },
                                isPulling = isPulling
                            )
                        }
                    }

                    // Pagination
                    if (totalPages > 1) {
                        Spacer(modifier = Modifier.height(Spacing.Small))
                        PaginationBar(
                            currentPage = currentPage,
                            totalPages = totalPages,
                            onPageChange = { currentPage = it }
                        )
                        Spacer(modifier = Modifier.height(Spacing.Small))
                    }
                }

                hasSearched -> {
                    // No results
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

                else -> {
                    // Initial state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
            }
        }
    }
}

@Composable
private fun PaginationBar(
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous button
        TextButton(
            onClick = { if (currentPage > 1) onPageChange(currentPage - 1) },
            enabled = currentPage > 1
        ) {
            Text(stringResource(R.string.pagination_prev))
        }

        // Page numbers
        val pageRange = when {
            totalPages <= 5 -> 1..totalPages
            currentPage <= 3 -> 1..5
            currentPage >= totalPages - 2 -> (totalPages - 4)..totalPages
            else -> (currentPage - 2)..(currentPage + 2)
        }

        pageRange.forEach { page ->
            TextButton(
                onClick = { onPageChange(page) },
                colors = if (page == currentPage) {
                    ButtonDefaults.textButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    ButtonDefaults.textButtonColors()
                },
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                Text("$page")
            }
        }

        // Next button
        TextButton(
            onClick = { if (currentPage < totalPages) onPageChange(currentPage + 1) },
            enabled = currentPage < totalPages
        ) {
            Text(stringResource(R.string.pagination_next))
        }
    }
}
