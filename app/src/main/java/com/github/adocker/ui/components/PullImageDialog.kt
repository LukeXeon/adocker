package com.github.adocker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.github.adocker.R
import com.github.adocker.ui.viewmodel.MainViewModel

@Composable
fun PullImageDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onNavigateToSearch: () -> Unit = {}
) {
    val isPulling by viewModel.isPulling.collectAsState()
    val pullProgress by viewModel.pullProgress.collectAsState()
    val error by viewModel.error.collectAsState()
    val message by viewModel.message.collectAsState()

    var imageName by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Show messages
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
                onDismiss()
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isPulling) onDismiss() },
        icon = { Icon(Icons.Default.Download, contentDescription = null) },
        title = { Text(stringResource(R.string.pull_image_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = imageName,
                    onValueChange = { imageName = it },
                    label = { Text(stringResource(R.string.pull_image_name_label)) },
                    placeholder = { Text(stringResource(R.string.pull_image_name_placeholder)) },
                    enabled = !isPulling,
                    trailingIcon = {
                        if (!isPulling) {
                            IconButton(onClick = {
                                onNavigateToSearch()
                                onDismiss()
                            }) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = stringResource(R.string.images_tab_search)
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (imageName.isNotBlank() && !isPulling) {
                                viewModel.pullImage(imageName)
                            }
                        }
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Pull Progress
                if (isPulling && pullProgress.isNotEmpty()) {
                    PullProgressCard(
                        imageName = imageName,
                        progress = pullProgress
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    focusManager.clearFocus()
                    if (imageName.isNotBlank()) {
                        viewModel.pullImage(imageName)
                    }
                },
                enabled = imageName.isNotBlank() && !isPulling
            ) {
                if (isPulling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isPulling) {
                        stringResource(R.string.pull_image_pulling)
                    } else {
                        stringResource(R.string.action_pull)
                    }
                )
            }
        },
        dismissButton = {
            if (!isPulling) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    )
}
