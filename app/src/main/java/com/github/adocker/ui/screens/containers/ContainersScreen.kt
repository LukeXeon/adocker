package com.github.adocker.ui.screens.containers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.github.adocker.R
import com.github.adocker.daemon.database.model.ContainerEntity
import com.github.adocker.daemon.database.model.ContainerStatus
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
    val error by viewModel.error.collectAsState()
    val message by viewModel.message.collectAsState()

    var filterStatus by remember { mutableStateOf<ContainerStatus?>(null) }
    var showDeleteDialog by remember { mutableStateOf<ContainerEntity?>(null) }

    val filteredContainers = remember(containers, filterStatus) {
        if (filterStatus == null) containers
        else containers.filter { it.status == filterStatus }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.containers_title)) },
                actions = {
                    // Filter dropdown
                    var expanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.containers_tab_all)) },
                            onClick = {
                                filterStatus = null
                                expanded = false
                            },
                            leadingIcon = {
                                if (filterStatus == null) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                        ContainerStatus.entries.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.name) },
                                onClick = {
                                    filterStatus = status
                                    expanded = false
                                },
                                leadingIcon = {
                                    if (filterStatus == status) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        if (containers.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.containers_empty),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.containers_empty_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status summary
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val running = containers.count { it.status == ContainerStatus.RUNNING }
                        val stopped = containers.count { it.status != ContainerStatus.RUNNING }

                        FilterChip(
                            selected = filterStatus == null,
                            onClick = { filterStatus = null },
                            label = { Text("${stringResource(R.string.containers_tab_all)} (${containers.size})") }
                        )
                        FilterChip(
                            selected = filterStatus == ContainerStatus.RUNNING,
                            onClick = {
                                filterStatus = if (filterStatus == ContainerStatus.RUNNING) null
                                else ContainerStatus.RUNNING
                            },
                            label = { Text("${stringResource(R.string.containers_tab_running)} ($running)") },
                            leadingIcon = {
                                if (filterStatus == ContainerStatus.RUNNING) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )
                        FilterChip(
                            selected = filterStatus == ContainerStatus.STOPPED,
                            onClick = {
                                filterStatus = if (filterStatus == ContainerStatus.STOPPED) null
                                else ContainerStatus.STOPPED
                            },
                            label = { Text("${stringResource(R.string.containers_tab_stopped)} ($stopped)") }
                        )
                    }
                }

                items(filteredContainers, key = { it.id }) { container ->
                    ContainerCard(
                        container = container,
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

    // Snackbar for errors/messages
    error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            viewModel.clearError()
        }
    }

    message?.let { msg ->
        LaunchedEffect(msg) {
            viewModel.clearMessage()
        }
    }
}
