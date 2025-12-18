package com.github.adocker.ui.screens.containers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.github.adocker.R
import com.github.adocker.daemon.containers.Container
import com.github.adocker.ui.theme.IconSize
import com.github.adocker.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainersScreen(
    onNavigateToTerminal: (String) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = hiltViewModel<ContainersViewModel>()

    val containers by viewModel.containers.collectAsState()

    val sortedContainers = remember(containers) {
        containers.asSequence().sortedBy { container -> container.key }
            .map { container ->
                container.value
            }.toList()
    }

    val containerStates = sortedContainers.map { container ->
        container.state.collectAsState().value
    }

    val statesCount = remember(sortedContainers, containerStates) {
        FilterType.entries.asSequence().map {
            it to sortedContainers.asSequence().map { container -> container.state.value }
                .filter(it.predicate).count()
        }.toMap()
    }

    var filterType by remember { mutableStateOf(FilterType.All) }

    val filteredContainers = remember(sortedContainers, filterType) {
        sortedContainers.asSequence()
            .filter { container -> filterType.predicate(container.state.value) }.toList()
    }

    var showDeleteDialog by remember { mutableStateOf<Container?>(null) }


    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.containers_title)) }
        )
        // Filter chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            items(FilterType.entries, { it }) { type ->
                FilterChip(
                    selected = filterType == type,
                    onClick = {
                        filterType = if (filterType == type && type != FilterType.All) {
                            FilterType.All
                        } else {
                            type
                        }
                    },
                    label = {
                        Text(
                            "${stringResource(type.labelResId)} (${
                                statesCount.getOrDefault(type, 0)
                            })"
                        )
                    },
                    leadingIcon = if (filterType == type) {
                        {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(IconSize.Small)
                            )
                        }
                    } else null
                )
            }
        }

        when {
            containers.isEmpty() -> {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = null,
                            modifier = Modifier.size(IconSize.Huge),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(Spacing.Medium))
                        Text(
                            text = stringResource(R.string.containers_empty),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(Spacing.Small))
                        Text(
                            text = stringResource(R.string.containers_empty_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            filteredContainers.isEmpty() -> {
                // No results for filter
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterListOff,
                            contentDescription = null,
                            modifier = Modifier.size(IconSize.ExtraLarge),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(Spacing.Medium))
                        Text(
                            text = stringResource(R.string.containers_filter_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = Spacing.ScreenPadding,
                        top = Spacing.Medium,
                        end = Spacing.ScreenPadding,
                        bottom = Spacing.Medium
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.ListItemSpacing)
                ) {
                    items(filteredContainers, key = { it.containerId }) { container ->
                        ContainerCard(
                            container = container,
                            onStart = { viewModel.startContainer(container.containerId) },
                            onStop = { viewModel.stopContainer(container.containerId) },
                            onDelete = { showDeleteDialog = container },
                            onTerminal = { onNavigateToTerminal(container.containerId) },
                            onClick = { onNavigateToDetail(container.containerId) }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { container ->
        var containerName by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(container) {
            container.getMetadata().onSuccess { entity ->
                containerName = entity.name
            }
        }

        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text(stringResource(R.string.containers_delete_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.containers_delete_confirm_message,
                        containerName ?: container.containerId
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteContainer(container.containerId)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
