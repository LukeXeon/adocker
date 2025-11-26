package com.github.adocker.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.github.adocker.R
import com.github.adocker.core.database.model.MirrorEntity
import com.github.adocker.ui.screens.qrcode.MirrorQRCode
import com.github.adocker.ui.viewmodel.MirrorSettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MirrorSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToQRScanner: () -> Unit,
    scannedMirrorData: String? = null,
) {
    val viewModel = hiltViewModel<MirrorSettingsViewModel>()
    val json = viewModel.json
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val mirrors by viewModel.allMirrors.collectAsState()
    val isChecking by viewModel.isCheckingHealth.collectAsState()

    // Handle scanned mirror data
    LaunchedEffect(scannedMirrorData) {
        if (scannedMirrorData != null) {
            try {
                val mirrorQRCode = json.runCatching {
                    decodeFromString<MirrorQRCode>(scannedMirrorData)
                }.getOrNull()
                if (mirrorQRCode != null) {
                    viewModel.addCustomMirror(
                        mirrorQRCode.name,
                        mirrorQRCode.url,
                        mirrorQRCode.bearerToken
                    )
                    snackbarHostState.showSnackbar("Mirror imported: ${mirrorQRCode.name}")
                } else {
                    snackbarHostState.showSnackbar("Invalid QR code format")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Failed to import mirror: ${e.message}")
            }
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var mirrorToDelete by remember { mutableStateOf<MirrorEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.mirror_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.checkMirrorsNow() },
                        enabled = !isChecking
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.width(24.dp).height(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Check Health"
                            )
                        }
                    }
                    IconButton(onClick = onNavigateToQRScanner) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = stringResource(R.string.mirror_settings_scan_qr)
                        )
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.mirror_settings_add)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.mirror_settings_auto_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(mirrors, key = { it.url }) { mirror ->
                MirrorCard(
                    mirror = mirror,
                    onDelete = if (!mirror.isBuiltIn) {
                        { mirrorToDelete = mirror }
                    } else null
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedCard(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.mirror_settings_add),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    // Add mirror dialog
    if (showAddDialog) {
        AddMirrorDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, url, token ->
                viewModel.addCustomMirror(name, url, token)
                showAddDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Mirror added: $name")
                }
            }
        )
    }

    // Delete confirmation dialog
    mirrorToDelete?.let { mirror ->
        AlertDialog(
            onDismissRequest = { mirrorToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text(stringResource(R.string.mirror_settings_delete_title)) },
            text = { Text(stringResource(R.string.mirror_settings_delete_message, mirror.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCustomMirror(mirror)
                        mirrorToDelete = null
                        scope.launch {
                            snackbarHostState.showSnackbar("Mirror deleted")
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { mirrorToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun MirrorCard(
    mirror: MirrorEntity,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Health status icon
            Icon(
                imageVector = if (mirror.isHealthy) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = if (mirror.isHealthy) "Healthy" else "Unhealthy",
                tint = if (mirror.isHealthy) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = mirror.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!mirror.isBuiltIn) {
                        AssistChip(
                            onClick = { },
                            label = { Text("Custom", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(24.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        )
                    }
                }

                Text(
                    text = mirror.url.removePrefix("https://"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Health details
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (mirror.latencyMs > 0) {
                        Text(
                            text = "Latency: ${mirror.latencyMs}ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    if (mirror.priority > 0) {
                        Text(
                            text = "Priority: ${mirror.priority}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!mirror.bearerToken.isNullOrEmpty()) {
                        AssistChip(
                            onClick = { },
                            label = { Text("Has Token", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(20.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }

            onDelete?.let {
                IconButton(onClick = it) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun AddMirrorDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, url: String, token: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://") }
    var token by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("50") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var urlError by remember { mutableStateOf<String?>(null) }

    // Get error messages in Composable context
    val nameRequiredError = "Name is required"
    val urlHintError = stringResource(R.string.mirror_settings_url_hint)
    val urlInvalidError = "Invalid URL"

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        title = { Text(stringResource(R.string.mirror_settings_add_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null
                    },
                    label = { Text(stringResource(R.string.mirror_settings_name_label)) },
                    placeholder = { Text(stringResource(R.string.mirror_settings_name_placeholder)) },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        urlError = null
                    },
                    label = { Text(stringResource(R.string.mirror_settings_url_label)) },
                    placeholder = { Text(stringResource(R.string.mirror_settings_url_placeholder)) },
                    isError = urlError != null,
                    supportingText = urlError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = priority,
                    onValueChange = { priority = it },
                    label = { Text("Priority (0-100)") },
                    placeholder = { Text("50") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text(stringResource(R.string.mirror_settings_token_label)) },
                    placeholder = { Text(stringResource(R.string.mirror_settings_token_placeholder)) },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    var hasError = false
                    if (name.isBlank()) {
                        nameError = nameRequiredError
                        hasError = true
                    }
                    if (!url.startsWith("https://") && !url.startsWith("http://")) {
                        urlError = urlHintError
                        hasError = true
                    } else if (url.length < 10) {
                        urlError = urlInvalidError
                        hasError = true
                    }
                    if (!hasError) {
                        val priorityInt = priority.toIntOrNull() ?: 50
                        onAdd(
                            name.trim(),
                            url.trim().removeSuffix("/"),
                            token.trim().takeIf { it.isNotEmpty() }
                        )
                    }
                }
            ) {
                Text(stringResource(R.string.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
