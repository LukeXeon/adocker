package com.github.andock.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.github.andock.ui.theme.Spacing

@Composable
fun <T : Any> PaginationColumn(
    items: LazyPagingItems<T>,
    empty: PaginationEmptyPlaceholder,
    key: ((item: @JvmSuppressWildcards T) -> Any),
    itemContent: @Composable LazyItemScope.(item: T) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            items.loadState.refresh is LoadState.Loading && items.itemCount == 0 -> {
                // Initial loading
                PaginationInitialPlaceholder()
            }

            items.loadState.refresh is LoadState.Error && items.itemCount == 0 -> {
                // Error state
                PaginationErrorPlaceholder(
                    error = (items.loadState.refresh as LoadState.Error).error,
                    onRetry = { items.retry() }
                )
            }

            items.itemCount == 0 -> {
                // No results
                empty.render()
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
                    items(
                        count = items.itemCount,
                        key = items.itemKey(key)
                    ) { index ->
                        val item = items[index]
                        if (item != null) {
                            itemContent(item)
                        }
                    }

                    // Loading indicator for pagination
                    if (items.loadState.append is LoadState.Loading) {
                        item {
                            PaginationLoadingItem()
                        }
                    }

                    // Error indicator for pagination
                    if (items.loadState.append is LoadState.Error) {
                        item {
                            PaginationErrorItem(
                                onRetry = { items.retry() }
                            )
                        }
                    }
                }
            }
        }
    }
}