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
import com.github.adocker.core.database.model.ContainerEntity
import com.github.adocker.core.database.model.ContainerStatus
import com.github.adocker.ui.viewmodel.MainViewModel
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
    val container = containers.find { it.id == containerId }

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (container == null) {
        // Container not found, navigate back
        LaunchedEffect(Unit) {
            onNavigateBack()
        }
        return
    }

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
                    if (container.status == ContainerStatus.RUNNING) {
                        IconButton(onClick = { onNavigateToTerminal(container.id) }) {
                            Icon(
                                Icons.Default.Terminal,
                                contentDescription = stringResource(R.string.action_terminal)
                            )
                        }
                    }
                    // Start/Stop button
                    if (container.status == ContainerStatus.RUNNING) {
                        IconButton(onClick = { viewModel.stopContainer(container.id) }) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = stringResource(R.string.action_stop)
                            )
                        }
                    } else {
                        IconButton(onClick = { viewModel.startContainer(container.id) }) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.action_start)
                            )
                        }
                    }
                    // Delete button (only for stopped containers)
                    if (container.status != ContainerStatus.RUNNING) {
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
            StatusChip(status = container.status)

            // Basic Information Card
            DetailCard(title = stringResource(R.string.common_basic_info)) {
                DetailRow(label = stringResource(R.string.common_name), value = container.name)
                DetailRow(label = stringResource(R.string.common_id), value = container.id)
                DetailRow(label = stringResource(R.string.container_image), value = container.imageName)
                DetailRow(label = stringResource(R.string.container_status), value = getStatusText(container.status))
                DetailRow(
                    label = stringResource(R.string.container_created),
                    value = formatDate(container.created)
                )
            }

            // Configuration Card
            DetailCard(title = stringResource(R.string.common_config)) {
                if (container.config.cmd.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.container_command),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = container.config.cmd.joinToString(" "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (container.config.workingDir.isNotEmpty()) {
                    DetailRow(
                        label = stringResource(R.string.container_working_dir),
                        value = container.config.workingDir
                    )
                }

                if (container.config.hostname.isNotEmpty()) {
                    DetailRow(
                        label = stringResource(R.string.container_hostname),
                        value = container.config.hostname
                    )
                }
            }

            // Environment Variables Card
            if (container.config.env.isNotEmpty()) {
                DetailCard(title = stringResource(R.string.container_env_vars)) {
                    container.config.env.forEach { (key, value) ->
                        Text(
                            text = "$key=$value",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Volume Mounts Card
            if (container.config.binds.isNotEmpty()) {
                DetailCard(title = stringResource(R.string.container_volumes)) {
                    container.config.binds.forEach { bind ->
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
                Text(stringResource(R.string.containers_delete_confirm_message, container.name))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteContainer(container.id)
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
private fun StatusChip(status: ContainerStatus) {
    val (text, color) = when (status) {
        ContainerStatus.CREATED -> stringResource(R.string.status_created) to MaterialTheme.colorScheme.secondary
        ContainerStatus.RUNNING -> stringResource(R.string.status_running) to MaterialTheme.colorScheme.primary
        ContainerStatus.STOPPED -> stringResource(R.string.status_stopped) to MaterialTheme.colorScheme.error
        ContainerStatus.PAUSED -> stringResource(R.string.status_paused) to MaterialTheme.colorScheme.tertiary
        ContainerStatus.EXITED -> stringResource(R.string.status_stopped) to MaterialTheme.colorScheme.error
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
private fun getStatusText(status: ContainerStatus): String {
    return when (status) {
        ContainerStatus.CREATED -> stringResource(R.string.status_created)
        ContainerStatus.RUNNING -> stringResource(R.string.status_running)
        ContainerStatus.STOPPED -> stringResource(R.string.status_stopped)
        ContainerStatus.PAUSED -> stringResource(R.string.status_paused)
        ContainerStatus.EXITED -> stringResource(R.string.status_stopped)
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
