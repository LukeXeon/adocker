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
import com.github.adocker.daemon.containers.Container
import com.github.adocker.daemon.containers.ContainerState
import com.github.adocker.ui.theme.Spacing
import com.github.adocker.ui.theme.IconSize
import com.github.adocker.R
import com.github.adocker.ui.components.ContainerCard
import com.github.adocker.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainersScreen(
    viewModel: MainViewModel,
    onNavigateToTerminal: (String) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val containers by viewModel.containers.collectAsState()

    // Filter type: null = all, "Running" = running, "Other" = non-running
    var filterType by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Container?>(null) }

    // Observe all container states to trigger recomposition when any state changes
    // This ensures UI updates in real-time when container states change
    val containerStates = containers.map { container ->
        container.state.collectAsState().value
    }

    // Calculate counts for each status based on current states
    val statusCounts = remember(containers, containerStates) {
        val running = containers.count { it.state.value is ContainerState.Running }
        val created = containers.count {
            val state = it.state.value
            state is ContainerState.Created || state is ContainerState.Starting
        }
        val exited = containers.count {
            val state = it.state.value
            state is ContainerState.Exited || state is ContainerState.Stopping ||
            state is ContainerState.Dead || state is ContainerState.Removing ||
            state is ContainerState.Removed
        }
        mapOf(
            null to containers.size,
            "Created" to created,
            "Running" to running,
            "Exited" to exited
        )
    }

    // Filter containers based on filter type
    val filteredContainers = remember(containers, filterType, containerStates) {
        if (filterType == null) {
            containers
        } else {
            containers.filter { container ->
                val state = container.state.value
                when (filterType) {
                    "Running" -> state is ContainerState.Running
                    "Created" -> state is ContainerState.Created || state is ContainerState.Starting
                    "Exited" -> state is ContainerState.Exited || state is ContainerState.Stopping ||
                               state is ContainerState.Dead || state is ContainerState.Removing ||
                               state is ContainerState.Removed
                    else -> true
                }
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
                    selected = filterType == null,
                    onClick = { filterType = null },
                    label = {
                        Text("${stringResource(R.string.containers_tab_all)} (${statusCounts[null] ?: 0})")
                    },
                    leadingIcon = if (filterType == null) {
                        { Icon(Icons.Default.Check, null, Modifier.size(IconSize.Small)) }
                    } else null
                )
            }

            // Created filter
            item {
                FilterChip(
                    selected = filterType == "Created",
                    onClick = {
                        filterType = if (filterType == "Created") null else "Created"
                    },
                    label = {
                        Text("${stringResource(R.string.containers_tab_created)} (${statusCounts["Created"] ?: 0})")
                    },
                    leadingIcon = if (filterType == "Created") {
                        { Icon(Icons.Default.Check, null, Modifier.size(IconSize.Small)) }
                    } else null
                )
            }

            // Running filter
            item {
                FilterChip(
                    selected = filterType == "Running",
                    onClick = {
                        filterType = if (filterType == "Running") null else "Running"
                    },
                    label = {
                        Text("${stringResource(R.string.containers_tab_running)} (${statusCounts["Running"] ?: 0})")
                    },
                    leadingIcon = if (filterType == "Running") {
                        { Icon(Icons.Default.Check, null, Modifier.size(IconSize.Small)) }
                    } else null
                )
            }

            // Exited filter
            item {
                FilterChip(
                    selected = filterType == "Exited",
                    onClick = {
                        filterType = if (filterType == "Exited") null else "Exited"
                    },
                    label = {
                        Text("${stringResource(R.string.containers_tab_exited)} (${statusCounts["Exited"] ?: 0})")
                    },
                    leadingIcon = if (filterType == "Exited") {
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

    // Delete confirmation dialog
    showDeleteDialog?.let { container ->
        var containerName by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(container) {
            container.getInfo().onSuccess { entity ->
                containerName = entity.name
            }
        }

        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text(stringResource(R.string.containers_delete_confirm_title)) },
            text = {
                Text(stringResource(R.string.containers_delete_confirm_message, containerName ?: container.containerId))
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
