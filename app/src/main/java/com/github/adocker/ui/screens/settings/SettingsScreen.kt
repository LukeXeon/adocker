package com.github.adocker.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.adocker.ui.theme.Spacing
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.adocker.R
import com.github.adocker.daemon.io.formatFileSize
import com.github.adocker.ui.screens.settings.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateToMirrorSettings: () -> Unit,
    onNavigateToPhantomProcess: () -> Unit = {},
) {
    val viewModel = hiltViewModel<SettingsViewModel>()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val storageUsage by viewModel.storageUsage.collectAsState()
    val prootVersion by viewModel.prootVersion.collectAsState()

    var showClearDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Network section
                SettingsSection(title = stringResource(R.string.settings_network)) {
                    SettingsClickableItem(
                        icon = Icons.Default.Cloud,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = stringResource(R.string.settings_registry_mirror),
                        subtitle = stringResource(R.string.settings_registry_mirror_subtitle),
                        onClick = onNavigateToMirrorSettings
                    )
                }

                // System section
                SettingsSection(title = stringResource(R.string.settings_system)) {
                    // Phantom Process (Android 12+ only)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        SettingsClickableItem(
                            icon = Icons.Default.Block,
                            iconTint = MaterialTheme.colorScheme.error,
                            title = stringResource(R.string.settings_phantom_process),
                            subtitle = stringResource(R.string.settings_phantom_process_subtitle),
                            onClick = onNavigateToPhantomProcess,
                            isWarning = true
                        )
                    }

                    SettingsItem(
                        icon = Icons.Default.PhoneAndroid,
                        title = stringResource(R.string.settings_platform),
                        subtitle = stringResource(
                            R.string.settings_platform_value,
                            android.os.Build.VERSION.RELEASE,
                            android.os.Build.VERSION.SDK_INT
                        )
                    )

                    SettingsItem(
                        icon = Icons.Default.Memory,
                        title = stringResource(R.string.settings_architecture),
                        subtitle = viewModel.architecture
                    )
                }

                // Storage section
                SettingsSection(title = stringResource(R.string.settings_storage)) {
                    SettingsItem(
                        icon = Icons.Default.Storage,
                        title = stringResource(R.string.settings_storage_usage),
                        subtitle = storageUsage?.let { formatFileSize(it) }
                            ?: stringResource(R.string.status_loading)
                    )

                    SettingsClickableItem(
                        icon = Icons.Default.DeleteSweep,
                        iconTint = MaterialTheme.colorScheme.error,
                        title = stringResource(R.string.settings_clear_data),
                        subtitle = stringResource(R.string.settings_clear_data_subtitle),
                        onClick = { showClearDialog = true },
                        isWarning = true
                    )
                }

                // About section
                SettingsSection(title = stringResource(R.string.settings_about)) {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.settings_version),
                        subtitle = viewModel.packageInfo.versionName ?: ""
                    )

                    SettingsItem(
                        icon = Icons.Default.Terminal,
                        title = stringResource(R.string.settings_engine),
                        subtitle = prootVersion ?: stringResource(R.string.terminal_unavailable)
                    )
                }

                // 呼吸空间
                Spacer(modifier = Modifier.height(Spacing.Medium))
            }
        }

        // Snackbar - 放在底部中央
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Clear data confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(R.string.settings_clear_data)) },
            text = { Text(stringResource(R.string.settings_clear_data_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    viewModel.context.getString(R.string.settings_clear_data_success)
                                )
                            }
                        }
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
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
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint
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
        }
    }
}

@Composable
private fun SettingsClickableItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    isWarning: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isWarning) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
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
