package com.github.andock.ui.screens.images

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import com.github.andock.R
import com.github.andock.daemon.database.model.ImageEntity
import com.github.andock.ui.components.DetailCard
import com.github.andock.ui.components.DetailRow
import com.github.andock.ui.components.LoadingDialog
import com.github.andock.ui.screens.containers.ContainerCreateRoute
import com.github.andock.ui.screens.main.LocalNavController
import com.github.andock.ui.utils.debounceClick
import com.github.andock.ui.utils.formatDate
import com.github.andock.ui.utils.formatSize
import com.github.andock.ui.utils.withAtLeast
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageDetailScreen() {
    val viewModel = hiltViewModel<ImageDetailViewModel>()
    val navController = LocalNavController.current
    val image = viewModel.image.collectAsState().value
    val (showDeleteDialog, setDeleteDialog) = remember { mutableStateOf<ImageEntity?>(null) }
    val (isLoading, setLoading) = remember { mutableStateOf(false) }
    val onNavigateBack = debounceClick {
        navController.popBackStack()
    }
    if (image != null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.nav_image_detail)) },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.common_back)
                            )
                        }
                    },
                    actions = {
                        // Run container button
                        IconButton(onClick = debounceClick {
                            navController.navigate(ContainerCreateRoute(image.id))
                        }) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.images_run)
                            )
                        }
                        // Delete button
                        IconButton(onClick = { setDeleteDialog(image) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.images_delete)
                            )
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
                // Basic Information Card
                DetailCard(title = stringResource(R.string.common_basic_info)) {
                    DetailRow(
                        label = stringResource(R.string.common_name),
                        value = image.fullName
                    )
                    DetailRow(
                        label = stringResource(R.string.images_repository),
                        value = image.repository
                    )
                    DetailRow(label = stringResource(R.string.images_tag), value = image.tag)
                    DetailRow(label = stringResource(R.string.images_id), value = image.id)
                    DetailRow(
                        label = stringResource(R.string.images_digest),
                        value = image.id.take(12)
                    )
                }

                // System Information Card
                DetailCard(title = stringResource(R.string.common_system_info)) {
                    DetailRow(
                        label = stringResource(R.string.images_architecture),
                        value = image.architecture
                    )
                    DetailRow(label = stringResource(R.string.images_os), value = image.os)
                    DetailRow(
                        label = stringResource(R.string.images_size),
                        value = formatSize(image.size)
                    )
                    DetailRow(
                        label = stringResource(R.string.images_created),
                        value = formatDate(image.created)
                    )
                }

                // Layers Card
                DetailCard(title = stringResource(R.string.images_layers)) {
                    Text(
                        text = stringResource(R.string.images_layer_count, image.layerIds.size),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    image.layerIds.forEachIndexed { index, layerId ->
                        Text(
                            text = "${index + 1}. ${layerId.take(12)}...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Configuration Card (if available)
                image.config?.let { config ->
                    DetailCard(title = stringResource(R.string.common_config)) {
                        config.env?.let { env ->
                            if (env.isNotEmpty()) {
                                Text(
                                    text = stringResource(R.string.container_env_vars),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                env.forEach { envVar ->
                                    Text(
                                        text = envVar,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                        config.cmd?.let { cmd ->
                            if (cmd.isNotEmpty()) {
                                Text(
                                    text = stringResource(R.string.container_command),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = cmd.joinToString(" "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                        config.workingDir?.let { workingDir ->
                            if (workingDir.isNotEmpty()) {
                                DetailRow(
                                    label = stringResource(R.string.container_working_dir),
                                    value = workingDir
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    // Delete confirmation dialog
    if (showDeleteDialog != null) {

        ImageDeleteDialog(
            showDeleteDialog,
            onDelete = {
                viewModel.viewModelScope.launch {
                    try {
                        setLoading(true)
                        setDeleteDialog(null)
                        withAtLeast(200) {
                            viewModel.deleteImage(it.id)
                        }
                    } finally {
                        setLoading(false)
                        onNavigateBack()
                    }
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
