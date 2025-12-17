package com.github.adocker.ui.screens.containers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.adocker.R
import com.github.adocker.daemon.containers.ContainerState
import com.github.adocker.daemon.database.model.ContainerEntity
import com.github.adocker.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainerDetailScreen(
    containerId: String,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTerminal: (String) -> Unit
) {
    val containers by viewModel.containers.collectAsState()
    val container = containers.find { it.containerId == containerId }
    val scope = rememberCoroutineScope()

    var containerInfo by remember { mutableStateOf<ContainerEntity?>(null) }

    // Load container info
    LaunchedEffect(container) {
        container?.let {
            scope.launch {
                it.getInfo().onSuccess { entity ->
                    containerInfo = entity
                }
            }
        }
    }

    // Observe container state in real-time
    val containerState = container?.state?.collectAsState()?.value

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (container == null || containerInfo == null) {
        // Container not found or info not loaded yet
        if (container == null) {
            LaunchedEffect(Unit) {
                onNavigateBack()
            }
        }
        return
    }

    val info = containerInfo!! // Safe because we checked above

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_container_detail)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                },
                actions = {
                    // Terminal button (only for running containers)
                    if (containerState is ContainerState.Running) {
                        IconButton(onClick = { onNavigateToTerminal(container.containerId) }) {
                            Icon(
                                Icons.Default.Terminal,
                                contentDescription = stringResource(R.string.action_terminal)
                            )
                        }
                    }
                    // Start/Stop button
                    if (containerState is ContainerState.Running) {
                        IconButton(onClick = { viewModel.stopContainer(container.containerId) }) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = stringResource(R.string.action_stop)
                            )
                        }
                    } else {
                        IconButton(onClick = { viewModel.startContainer(container.containerId) }) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.action_start)
                            )
                        }
                    }
                    // Delete button (only for stopped containers)
                    if (containerState !is ContainerState.Running) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.action_delete)
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status badge
            containerState?.let { StatusChip(state = it) }

            // Basic Information Card
            DetailCard(title = stringResource(R.string.common_basic_info)) {
                DetailRow(label = stringResource(R.string.common_name), value = info.name)
                DetailRow(label = stringResource(R.string.common_id), value = info.id)
                DetailRow(label = stringResource(R.string.container_image), value = info.imageName)
                DetailRow(label = stringResource(R.string.container_status), value = containerState?.let { getStatusText(it) } ?: "Unknown")
                DetailRow(
                    label = stringResource(R.string.container_created),
                    value = formatDate(info.createdAt)
                )
            }

            // Configuration Card
            DetailCard(title = stringResource(R.string.common_config)) {
                if (info.config.cmd.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.container_command),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = info.config.cmd.joinToString(" "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (info.config.workingDir.isNotEmpty()) {
                    DetailRow(
                        label = stringResource(R.string.container_working_dir),
                        value = info.config.workingDir
                    )
                }

                if (info.config.hostname.isNotEmpty()) {
                    DetailRow(
                        label = stringResource(R.string.container_hostname),
                        value = info.config.hostname
                    )
                }
            }

            // Environment Variables Card
            if (info.config.env.isNotEmpty()) {
                DetailCard(title = stringResource(R.string.container_env_vars)) {
                    info.config.env.forEach { (key, value) ->
                        Text(
                            text = "$key=$value",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Volume Mounts Card
            if (info.config.binds.isNotEmpty()) {
                DetailCard(title = stringResource(R.string.container_volumes)) {
                    info.config.binds.forEach { bind ->
                        Text(
                            text = "${bind.hostPath} -> ${bind.containerPath}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text(stringResource(R.string.containers_delete_confirm_title)) },
            text = {
                Text(stringResource(R.string.containers_delete_confirm_message, info.name))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteContainer(container.containerId)
                        showDeleteDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun StatusChip(state: ContainerState) {
    val (text, color) = when (state) {
        is ContainerState.Created -> "Created" to MaterialTheme.colorScheme.secondary
        is ContainerState.Starting -> "Starting" to MaterialTheme.colorScheme.secondary
        is ContainerState.Running -> "Running" to MaterialTheme.colorScheme.primary
        is ContainerState.Stopping -> "Stopping" to MaterialTheme.colorScheme.error
        is ContainerState.Exited -> "Exited" to MaterialTheme.colorScheme.error
        is ContainerState.Dead -> "Dead" to MaterialTheme.colorScheme.error
        is ContainerState.Removing -> "Removing" to MaterialTheme.colorScheme.error
        is ContainerState.Removed -> "Removed" to MaterialTheme.colorScheme.error
    }

    AssistChip(
        onClick = { },
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            labelColor = color
        )
    )
}

@Composable
private fun DetailCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun getStatusText(state: ContainerState): String {
    return when (state) {
        is ContainerState.Created -> "Created"
        is ContainerState.Starting -> "Starting"
        is ContainerState.Running -> "Running"
        is ContainerState.Stopping -> "Stopping"
        is ContainerState.Exited -> "Exited"
        is ContainerState.Dead -> "Dead"
        is ContainerState.Removing -> "Removing"
        is ContainerState.Removed -> "Removed"
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
