package com.adocker.runner.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.adocker.runner.core.config.Config
import com.adocker.runner.core.utils.FileUtils
import com.adocker.runner.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToMirrorSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val storageUsage by viewModel.storageUsage.collectAsState()
    val prootVersion by viewModel.prootVersion.collectAsState()
    val currentMirror by viewModel.currentMirror.collectAsState()

    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Registry Mirror section (important for China users)
            SettingsSection(title = "Registry Mirror") {
                Surface(
                    onClick = onNavigateToMirrorSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Docker Registry Mirror",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = currentMirror?.name ?: "Loading...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Tap to manage mirrors for faster downloads",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // About section
            SettingsSection(title = "About") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Version",
                    subtitle = Config.VERSION
                )
                SettingsItem(
                    icon = Icons.Default.Sailing,
                    title = "App Name",
                    subtitle = Config.APP_NAME
                )
                SettingsItem(
                    icon = Icons.Default.Memory,
                    title = "Architecture",
                    subtitle = Config.getArchitecture()
                )
            }

            // Engine section
            SettingsSection(title = "Execution Engine") {
                SettingsItem(
                    icon = Icons.Default.Terminal,
                    title = "PRoot Version",
                    subtitle = prootVersion ?: "Not available"
                )
                SettingsItem(
                    icon = Icons.Default.Settings,
                    title = "Default Mode",
                    subtitle = "P1 (SECCOMP enabled)"
                )
            }

            // Storage section
            SettingsSection(title = "Storage") {
                SettingsItem(
                    icon = Icons.Default.Storage,
                    title = "Storage Usage",
                    subtitle = storageUsage?.let { FileUtils.formatSize(it) } ?: "Calculating..."
                )
                SettingsItem(
                    icon = Icons.Default.Folder,
                    title = "Data Directory",
                    subtitle = Config.baseDir.absolutePath
                )

                // Clear data button
                Surface(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Clear All Data",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Remove all images, containers, and cached data",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Advanced section
            SettingsSection(title = "Advanced") {
                SettingsItem(
                    icon = Icons.Default.BugReport,
                    title = "Debug Mode",
                    subtitle = "Disabled",
                    trailing = {
                        Switch(
                            checked = false,
                            onCheckedChange = { /* TODO */ },
                            enabled = false
                        )
                    }
                )
                SettingsItem(
                    icon = Icons.Default.Dns,
                    title = "DNS Servers",
                    subtitle = "8.8.8.8, 8.8.4.4"
                )
            }

            // Info section
            SettingsSection(title = "Information") {
                SettingsItem(
                    icon = Icons.Default.Code,
                    title = "Based on",
                    subtitle = "udocker concepts"
                )
                SettingsItem(
                    icon = Icons.Default.Android,
                    title = "Platform",
                    subtitle = "Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})"
                )
                SettingsItem(
                    icon = Icons.Default.PhoneAndroid,
                    title = "Device",
                    subtitle = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Clear data confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Clear All Data") },
            text = {
                Text("This will delete all images, containers, and cached data. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData {
                            scope.launch {
                                snackbarHostState.showSnackbar("All data cleared")
                            }
                        }
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            trailing?.invoke()
        }
    }
}
