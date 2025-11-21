package com.adocker.runner.ui.screens.images

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.adocker.runner.ui.components.PullProgressCard
import com.adocker.runner.ui.components.SearchResultCard
import com.adocker.runner.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullImageScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToQRScanner: () -> Unit = {},
    scannedImageName: String? = null,
    modifier: Modifier = Modifier
) {
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isPulling by viewModel.isPulling.collectAsState()
    val pullProgress by viewModel.pullProgress.collectAsState()
    val error by viewModel.error.collectAsState()
    val message by viewModel.message.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var pullImageName by remember { mutableStateOf("") }
    var showPullDialog by remember { mutableStateOf(false) }

    // Handle scanned image from QR code
    LaunchedEffect(scannedImageName) {
        if (scannedImageName != null) {
            pullImageName = scannedImageName
        }
    }

    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Show messages in snackbar
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
            if (it.contains("successfully")) {
                onNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pull Image") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToQRScanner) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR Code")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Direct pull input
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Pull Image",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pullImageName,
                        onValueChange = { pullImageName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Image name") },
                        placeholder = { Text("e.g., alpine:latest, ubuntu:22.04") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (pullImageName.isNotBlank() && !isPulling) {
                                    viewModel.pullImage(pullImageName)
                                    focusManager.clearFocus()
                                }
                            }
                        ),
                        enabled = !isPulling
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (pullImageName.isNotBlank()) {
                                viewModel.pullImage(pullImageName)
                                focusManager.clearFocus()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = pullImageName.isNotBlank() && !isPulling
                    ) {
                        if (isPulling) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pulling...")
                        } else {
                            Icon(
                                Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pull")
                        }
                    }
                }
            }

            // Pull progress
            if (isPulling && pullProgress.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                PullProgressCard(
                    imageName = pullImageName,
                    progress = pullProgress
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Search section
            Text(
                text = "Search Docker Hub",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search images...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        viewModel.searchImages(searchQuery)
                        focusManager.clearFocus()
                    }
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.searchImages(searchQuery)
                    focusManager.clearFocus()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = searchQuery.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Search")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search results
            if (searchResults.isNotEmpty()) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchResults) { result ->
                        SearchResultCard(
                            result = result,
                            onPull = {
                                pullImageName = result.name
                                showPullDialog = true
                            }
                        )
                    }
                }
            } else if (!isLoading && searchQuery.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No results found",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Pull confirmation dialog
    if (showPullDialog) {
        AlertDialog(
            onDismissRequest = { showPullDialog = false },
            icon = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
            title = { Text("Pull Image") },
            text = {
                Column {
                    Text("Pull image '$pullImageName'?")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pullImageName,
                        onValueChange = { pullImageName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Image name:tag") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.pullImage(pullImageName)
                        showPullDialog = false
                    }
                ) {
                    Text("Pull")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPullDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
