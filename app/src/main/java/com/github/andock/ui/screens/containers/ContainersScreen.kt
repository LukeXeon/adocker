package com.github.andock.ui.screens.containers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.viewModelScope
import com.github.andock.R
import com.github.andock.daemon.containers.Container
import com.github.andock.daemon.containers.ContainerState
import com.github.andock.ui.components.LoadingDialog
import com.github.andock.ui.screens.main.LocalNavController
import com.github.andock.ui.theme.IconSize
import com.github.andock.ui.theme.Spacing
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainersScreen() {
    val navController = LocalNavController.current
    val viewModel = hiltViewModel<ContainersViewModel>()
    val containers by viewModel.sortedList.collectAsState()
    val (isLoading, setLoading) = remember { mutableStateOf(false) }
    val statesCount by remember(containers) {
        combine(containers.map { it.state }) { states ->
            ContainerFilterType.entries.asSequence().map {
                it to states.asSequence().filter(it.predicate).count()
            }.toMap()
        }
    }.collectAsState(emptyMap())
    var filterType by remember { mutableStateOf(ContainerFilterType.All) }
    val filteredContainers by remember(filterType) {
        viewModel.stateList(filterType.predicate)
    }.collectAsState(emptyList())
    val (showDeleteDialog, setDeleteDialog) = remember { mutableStateOf<Container?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.containers_title)) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                items(ContainerFilterType.entries, { it }) { type ->
                    FilterChip(
                        selected = filterType == type,
                        onClick = {
                            filterType =
                                if (filterType == type && type != ContainerFilterType.All) {
                                    ContainerFilterType.All
                                } else {
                                    type
                                }
                        },
                        label = {
                            Text(
                                "${type.label} (${statesCount.getOrDefault(type, 0)})"
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
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = Spacing.BottomSpacing),
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
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = Spacing.BottomSpacing),
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
                        items(filteredContainers, key = { it.id }) { container ->
                            ContainerCard(
                                container = container,
                                onStart = { viewModel.startContainer(container.id) },
                                onStop = { viewModel.stopContainer(container.id) },
                                onDelete = { setDeleteDialog(container) },
                                onTerminal = {
                                    navController.navigate(
                                        ContainerExecRoute(container.id)
                                    )
                                },
                                onClick = {
                                    navController.navigate(
                                        ContainerDetailRoute(container.id)
                                    )
                                }
                            )
                        }
                        item {
                            Spacer(Modifier.height(Spacing.BottomSpacing))
                        }
                    }
                }
            }
        }
    }
    if (showDeleteDialog != null) {
        ContainerDeleteDialog(
            showDeleteDialog,
            onDelete = {
                viewModel.viewModelScope.launch {
                    setLoading(true)
                    setDeleteDialog(null)
                    viewModel.deleteContainer(it.id)
                    showDeleteDialog.state.filterIsInstance<ContainerState.Removed>().first()
                    setLoading(false)
                }
            },
            onDismissRequest = {
                setDeleteDialog(null)
            }
        )
    }
    if (isLoading) {
        LoadingDialog()
    }
}
