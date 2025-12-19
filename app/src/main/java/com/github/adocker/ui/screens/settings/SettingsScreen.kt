package com.github.adocker.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.github.adocker.R
import com.github.adocker.daemon.app.AppContext
import com.github.adocker.daemon.io.formatFileSize
import com.github.adocker.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateToMirrorSettings: () -> Unit,
    onNavigateToPhantomProcess: () -> Unit = {},
) {
    val viewModel = hiltViewModel<SettingsViewModel>()
    val snackbarHostState = remember { SnackbarHostState() }

    val storageUsage by viewModel.storageUsage.collectAsState()
    val prootVersion = viewModel.prootVersion

    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadStorageUsage()
    }

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
                        subtitle = AppContext.ARCHITECTURE
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
                            snackbarHostState.showSnackbar(
                                viewModel.context.getString(R.string.settings_clear_data_success)
                            )
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
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant
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
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
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
