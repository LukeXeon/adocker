package com.github.andock.ui.screens.containers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import com.github.andock.R
import com.github.andock.daemon.containers.Container
import com.github.andock.daemon.containers.ContainerState
import com.github.andock.ui.components.DetailCard
import com.github.andock.ui.components.DetailRow
import com.github.andock.ui.components.LoadingDialog
import com.github.andock.ui.screens.main.LocalNavigator
import com.github.andock.ui.utils.debounceClick
import com.github.andock.ui.utils.formatDate
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainerDetailScreen(navKey: ContainerDetailKey) {
    val viewModel = hiltViewModel<ContainerDetailViewModel, ContainerDetailViewModel.Factory> { factory ->
        factory.create(navKey)
    }
    val (isLoading, setLoading) = remember { mutableStateOf(false) }
    val container = viewModel.container.collectAsState().value ?: return
    val metadata = container.metadata.collectAsState().value ?: return
    val containerState by container.state.collectAsState()
    val (showDeleteDialog, setDeleteDialog) = remember { mutableStateOf<Container?>(null) }
    val navigator = LocalNavigator.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_container_detail)) },
                navigationIcon = {
                    IconButton(
                        onClick = debounceClick {
                            navigator.goBack()
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                },
                actions = {
                    // Terminal button (only for running containers)
                    if (containerState is ContainerState.Running) {
                        IconButton(onClick = {
                            navigator.navigate(ContainerExecKey(container.id))
                        }) {
                            Icon(
                                Icons.Default.Terminal,
                                contentDescription = stringResource(R.string.action_terminal)
                            )
                        }
                    }
                    // Start/Stop button
                    if (containerState is ContainerState.Running) {
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
                    if (containerState !is ContainerState.Running) {
                        IconButton(onClick = { setDeleteDialog(container) }) {
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
            ContainerStateChip(state = containerState)

            // Basic Information Card
            DetailCard(title = stringResource(R.string.common_basic_info)) {
                DetailRow(label = stringResource(R.string.common_name), value = metadata.name)
                DetailRow(label = stringResource(R.string.common_id), value = metadata.id)
                DetailRow(
                    label = stringResource(R.string.container_image),
                    value = metadata.imageName
                )
                DetailRow(
                    label = stringResource(R.string.container_status),
                    value = containerState::class.simpleName ?: ""
                )
                DetailRow(
                    label = stringResource(R.string.container_created),
                    value = formatDate(metadata.createdAt)
                )
            }

            // Configuration Card
            DetailCard(title = stringResource(R.string.common_config)) {
                if (metadata.config.cmd.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.container_command),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = metadata.config.cmd.joinToString(" "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (metadata.config.workingDir.isNotEmpty()) {
                    DetailRow(
                        label = stringResource(R.string.container_working_dir),
                        value = metadata.config.workingDir
                    )
                }

                if (metadata.config.hostname.isNotEmpty()) {
                    DetailRow(
                        label = stringResource(R.string.container_hostname),
                        value = metadata.config.hostname
                    )
                }
            }

            // Environment Variables Card
            if (metadata.config.env.isNotEmpty()) {
                DetailCard(title = stringResource(R.string.container_env_vars)) {
                    metadata.config.env.forEach { (key, value) ->
                        Text(
                            text = "$key=$value",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Volume Mounts Card
            if (metadata.config.binds.isNotEmpty()) {
                DetailCard(title = stringResource(R.string.container_volumes)) {
                    metadata.config.binds.forEach { bind ->
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