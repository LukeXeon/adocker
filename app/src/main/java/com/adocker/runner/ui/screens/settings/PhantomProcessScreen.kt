package com.adocker.runner.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.adocker.runner.R
import com.adocker.runner.ui.viewmodel.PhantomProcessViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhantomProcessScreen(
    onNavigateBack: () -> Unit,
) {
    val viewModel = hiltViewModel<PhantomProcessViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error or success messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                actionLabel = "Dismiss",
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.phantom_title)) },
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info card
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.phantom_no_restriction),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                // Status overview card
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.phantom_status),
                            style = MaterialTheme.typography.titleMedium
                        )

                        HorizontalDivider()

                        // Shizuku status
                        StatusRow(
                            label = "Shizuku",
                            status = when {
                                !uiState.shizukuAvailable -> stringResource(R.string.phantom_shizuku_not_running)
                                !uiState.shizukuPermissionGranted -> stringResource(R.string.phantom_permission_denied)
                                else -> "Ready"
                            },
                            isGood = uiState.shizukuAvailable && uiState.shizukuPermissionGranted,
                            icon = Icons.Default.Security
                        )

                        // Phantom process restriction status
                        StatusRow(
                            label = stringResource(R.string.phantom_current_limit),
                            status = when {
                                uiState.isChecking -> stringResource(R.string.status_loading)
                                uiState.phantomKillerDisabled -> "Disabled"
                                else -> "Active (limit: ${uiState.currentLimit ?: "32"})"
                            },
                            isGood = uiState.phantomKillerDisabled,
                            icon = Icons.Default.Block
                        )
                    }
                }

                // Action buttons
                when {
                    !uiState.shizukuAvailable -> {
                        // Shizuku not installed
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = stringResource(R.string.phantom_shizuku_required),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.phantom_shizuku_message),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Button(
                                    onClick = {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://github.com/RikkaApps/Shizuku/releases")
                                        )
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.phantom_shizuku_install))
                                }
                            }
                        }
                    }

                    !uiState.shizukuPermissionGranted -> {
                        // Shizuku permission needed
                        Card {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.phantom_permission_denied),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = stringResource(R.string.phantom_permission_message),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Button(
                                    onClick = { viewModel.requestShizukuPermission() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Grant Permission")
                                }
                            }
                        }
                    }

                    !uiState.phantomKillerDisabled -> {
                        // Phantom killer active - offer to disable
                        Card {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Restrictions Active",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Android is currently limiting child processes. This may cause containers to terminate unexpectedly.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Button(
                                    onClick = { viewModel.disablePhantomKiller() },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !uiState.isProcessing
                                ) {
                                    if (uiState.isProcessing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Icon(Icons.Default.Block, contentDescription = null)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text("Disable Restrictions")
                                }
                            }
                        }
                    }

                    else -> {
                        // Phantom killer disabled - success state
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Restrictions Disabled",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Text(
                                    text = "Phantom process restrictions are disabled. Containers can run normally.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                OutlinedButton(
                                    onClick = { viewModel.enablePhantomKiller() },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !uiState.isProcessing
                                ) {
                                    if (uiState.isProcessing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(Icons.Default.Restore, contentDescription = null)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text("Restore Default")
                                }
                            }
                        }
                    }
                }

                // Help section
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "How to Use",
                            style = MaterialTheme.typography.titleMedium
                        )

                        HorizontalDivider()

                        HelpStep(
                            number = "1",
                            title = "Install Shizuku",
                            description = "Download and install Shizuku from GitHub releases"
                        )

                        HelpStep(
                            number = "2",
                            title = "Start Shizuku",
                            description = "Connect your device to PC and run:\nadb shell sh /storage/emulated/0/Android/data/moe.shizuku.privileged.api/start.sh",
                            code = true
                        )

                        HelpStep(
                            number = "3",
                            title = "Grant Permission",
                            description = "Return to this app and tap 'Grant Permission'"
                        )

                        HelpStep(
                            number = "4",
                            title = "Disable Restrictions",
                            description = "Tap 'Disable Restrictions' to allow unlimited container processes"
                        )
                    }
                }

                // Alternative methods card
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Alternative Methods",
                            style = MaterialTheme.typography.titleMedium
                        )

                        HorizontalDivider()

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            Text(
                                text = "Manual Setting (Android 14+)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Settings → Developer Options → Disable child process restrictions",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        Text(
                            text = "Direct ADB Command",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val adbCommand = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
                            "adb shell settings put global settings_enable_monitor_phantom_procs false"
                        } else {
                            "adb shell device_config put activity_manager max_phantom_processes 2147483647"
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = adbCommand,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    status: String,
    isGood: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isGood) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isGood) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = if (isGood) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

@Composable
private fun HelpStep(
    number: String,
    title: String,
    description: String,
    code: Boolean = false
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (code) {
                Spacer(Modifier.height(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = description,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
