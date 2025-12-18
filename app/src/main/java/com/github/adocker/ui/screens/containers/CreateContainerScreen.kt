package com.github.adocker.ui.screens.containers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.github.adocker.ui.theme.Spacing
import com.github.adocker.ui.theme.IconSize
import com.github.adocker.R
import com.github.adocker.daemon.registry.model.ContainerConfig
import com.github.adocker.ui.screens.images.ImagesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateContainerScreen(
    imageId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val imagesViewModel = hiltViewModel<ImagesViewModel>()
    val containersViewModel = hiltViewModel<ContainersViewModel>()
    val images by imagesViewModel.images.collectAsState()
    val image = remember(images, imageId) { images.find { it.id == imageId } }
//    val error by viewModel.error.collectAsState()
//    val message by viewModel.message.collectAsState()

    var containerName by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }
    var workingDir by remember { mutableStateOf("/") }
    var envVars by remember { mutableStateOf("") }
    var hostname by remember { mutableStateOf("localhost") }
    var autoStart by remember { mutableStateOf(false) }

//
//    LaunchedEffect(error) {
//        error?.let {
//            snackbarHostState.showSnackbar(it)
//            viewModel.clearError()
//        }
//    }
//
//    LaunchedEffect(message) {
//        message?.let {
//            snackbarHostState.showSnackbar(it)
//            viewModel.clearMessage()
//            if (it.contains("created") || it.contains("running")) {
//                onNavigateBack()
//            }
//        }
//    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_container_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        },
//        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
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

private fun parseEnvVars(input: String): Map<String, String> {
    return input.lines()
        .filter { it.isNotBlank() && it.contains("=") }
        .associate { line ->
            val parts = line.split("=", limit = 2)
            parts[0].trim() to parts.getOrElse(1) { "" }.trim()
        }
}
