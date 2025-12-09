package com.github.adocker.ui.screens.containers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.adocker.ui.theme.Spacing
import com.github.adocker.ui.theme.IconSize
import com.github.adocker.R
import com.github.adocker.daemon.containers.Container2
import com.github.adocker.ui.model.ContainerStatus
import com.github.adocker.ui.components.ContainerCard
import com.github.adocker.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainersScreen(
    viewModel: MainViewModel,
    onNavigateToTerminal: (String) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val containers by viewModel.containers.collectAsState()

    var filterStatus by remember { mutableStateOf<ContainerStatus?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Container2?>(null) }

    // Calculate counts for each status
    val statusCounts = remember(containers) {
        val running = containers.count { viewModel.getContainerStatus(it) == ContainerStatus.RUNNING }
        val created = containers.count { viewModel.getContainerStatus(it) == ContainerStatus.CREATED }
        val exited = containers.count { viewModel.getContainerStatus(it) == ContainerStatus.EXITED }
        mapOf(
            null to containers.size,
            ContainerStatus.CREATED to created,
            ContainerStatus.RUNNING to running,
            ContainerStatus.EXITED to exited
        )
    }

    // Filter containers based on status
    val filteredContainers = remember(containers, filterStatus) {
        if (filterStatus == null) {
            containers
        } else {
            containers.filter { container ->
                viewModel.getContainerStatus(container) == filterStatus
            }
        }
    }

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
            // All filter
            item {
                FilterChip(
                    selected = filterStatus == null,
                    onClick = { filterStatus = null },
                    label = {
                        Text("${stringResource(R.string.containers_tab_all)} (${statusCounts[null] ?: 0})")
                    },
                    leadingIcon = if (filterStatus == null) {
                        { Icon(Icons.Default.Check, null, Modifier.size(IconSize.Small)) }
                    } else null
                )
            }

            // Created filter
            item {
                FilterChip(
                    selected = filterStatus == ContainerStatus.CREATED,
                    onClick = {
                        filterStatus = if (filterStatus == ContainerStatus.CREATED) null
                        else ContainerStatus.CREATED
                    },
                    label = {
                        Text("${stringResource(R.string.containers_tab_created)} (${statusCounts[ContainerStatus.CREATED] ?: 0})")
                    },
                    leadingIcon = if (filterStatus == ContainerStatus.CREATED) {
                        { Icon(Icons.Default.Check, null, Modifier.size(IconSize.Small)) }
                    } else null
                )
            }

            // Running filter
            item {
                FilterChip(
                    selected = filterStatus == ContainerStatus.RUNNING,
                    onClick = {
                        filterStatus = if (filterStatus == ContainerStatus.RUNNING) null
                        else ContainerStatus.RUNNING
                    },
                    label = {
                        Text("${stringResource(R.string.containers_tab_running)} (${statusCounts[ContainerStatus.RUNNING] ?: 0})")
                    },
                    leadingIcon = if (filterStatus == ContainerStatus.RUNNING) {
                        { Icon(Icons.Default.Check, null, Modifier.size(IconSize.Small)) }
                    } else null
                )
            }

            // Exited filter
            item {
                FilterChip(
                    selected = filterStatus == ContainerStatus.EXITED,
                    onClick = {
                        filterStatus = if (filterStatus == ContainerStatus.EXITED) null
                        else ContainerStatus.EXITED
                    },
                    label = {
                        Text("${stringResource(R.string.containers_tab_exited)} (${statusCounts[ContainerStatus.EXITED] ?: 0})")
                    },
                    leadingIcon = if (filterStatus == ContainerStatus.EXITED) {
                        { Icon(Icons.Default.Check, null, Modifier.size(IconSize.Small)) }
                    } else null
                )
            }
        }

        if (containers.isEmpty()) {
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
        } else if (filteredContainers.isEmpty()) {
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
        } else {
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
                        status = viewModel.getContainerStatus(container),
                        onStart = { viewModel.startContainer(container.id) },
                        onStop = { viewModel.stopContainer(container.id) },
                        onDelete = { showDeleteDialog = container },
                        onTerminal = { onNavigateToTerminal(container.id) },
                        onClick = { onNavigateToDetail(container.id) }
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { container ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text(stringResource(R.string.containers_delete_confirm_title)) },
            text = {
                Text(stringResource(R.string.containers_delete_confirm_message, container.name))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteContainer(container.id)
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
