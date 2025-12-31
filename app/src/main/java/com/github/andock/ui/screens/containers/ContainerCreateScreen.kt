package com.github.andock.ui.screens.containers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.toRoute
import com.github.andock.R
import com.github.andock.daemon.images.model.ContainerConfig
import com.github.andock.ui.screens.images.ImagesViewModel
import com.github.andock.ui.screens.main.LocalNavController
import com.github.andock.ui.theme.IconSize
import com.github.andock.ui.theme.Spacing
import com.github.andock.ui.utils.debounceClick
import com.github.andock.ui.utils.parseEnvVars

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainerCreateScreen() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val route = remember {
        (lifecycleOwner as? NavBackStackEntry)?.toRoute<ContainerCreateRoute>()
    } ?: return
    val imageId = route.imageId
    val navController = LocalNavController.current
    val imagesViewModel = hiltViewModel<ImagesViewModel>()
    val containersViewModel = hiltViewModel<ContainersViewModel>()
    val images by imagesViewModel.images.collectAsState()
    val image = remember(images, imageId) { images.find { it.id == imageId } }
    var containerName by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }
    var workingDir by remember { mutableStateOf("/") }
    var envVars by remember { mutableStateOf("") }
    var hostname by remember { mutableStateOf("localhost") }
    var autoStart by remember { mutableStateOf(false) }
    val onNavigateBack = debounceClick {
        navController.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_container_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        },
    ) { padding ->
        if (image == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.create_container_image_not_found))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.Medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
            ) {
                // Image info card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.Medium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.ListItemSpacing)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = image.repository.removePrefix("library/"),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = image.tag,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Container name
                OutlinedTextField(
                    value = containerName,
                    onValueChange = { containerName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.create_container_name_label)) },
                    placeholder = { Text(stringResource(R.string.create_container_name_placeholder)) },
                    leadingIcon = {
                        Icon(
                            Icons.AutoMirrored.Filled.Label,
                            contentDescription = null
                        )
                    },
                    singleLine = true
                )

                // Command
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.create_container_command_label)) },
                    placeholder = { Text(stringResource(R.string.create_container_command_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Terminal, contentDescription = null) },
                    singleLine = true,
                    supportingText = { Text(stringResource(R.string.create_container_command_placeholder)) }
                )

                // Working directory
                OutlinedTextField(
                    value = workingDir,
                    onValueChange = { workingDir = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.create_container_workdir_label)) },
                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    singleLine = true
                )

                // Hostname
                OutlinedTextField(
                    value = hostname,
                    onValueChange = { hostname = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.create_container_hostname_label)) },
                    leadingIcon = { Icon(Icons.Default.Computer, contentDescription = null) },
                    singleLine = true
                )

                // Environment variables
                OutlinedTextField(
                    value = envVars,
                    onValueChange = { envVars = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.create_container_environment)) },
                    placeholder = { Text("KEY=value, one per line") },
                    leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) },
                    minLines = 3,
                    maxLines = 5,
                    supportingText = { Text("Format: KEY=value, one per line") }
                )

                // Auto start option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Start after creation",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Automatically run the container",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoStart,
                        onCheckedChange = { autoStart = it }
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.Medium))

                // Create button
                Button(
                    onClick = {
                        val config = ContainerConfig(
                            cmd = if (command.isNotBlank()) {
                                command.split(" ")
                            } else {
                                listOf("/bin/sh")
                            },
                            workingDir = workingDir.ifBlank { "/" },
                            env = parseEnvVars(envVars),
                            hostname = hostname.ifBlank { "localhost" },
                        )
                        if (autoStart) {
                            containersViewModel.runContainer(
                                imageId = imageId,
                                name = containerName.ifBlank { null },
                                config = config
                            )
                        } else {
                            containersViewModel.createContainer(
                                imageId = imageId,
                                name = containerName.ifBlank { null },
                                config = config
                            )
                        }
                        onNavigateBack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (autoStart) Icons.Default.PlayArrow else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.Small)
                    )
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    Text(
                        stringResource(
                            if (autoStart) {
                                R.string.action_run
                            } else {
                                R.string.create_container_button
                            }
                        )
                    )
                }
            }
        }
    }
}
