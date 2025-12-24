package com.github.andock.ui.screens.permissions

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.github.andock.R
import com.github.andock.ui.components.HelpStep
import com.github.andock.ui.components.StatusRow
import com.github.andock.ui.theme.IconSize
import com.github.andock.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhantomProcessScreen(
    onNavigateBack: () -> Unit = {},
) {
    val viewModel = hiltViewModel<PhantomProcessViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error or success messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                actionLabel = context.getString(R.string.action_dismiss),
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
                .padding(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
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
                            .padding(Spacing.Medium),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.ListItemSpacing),
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
                        modifier = Modifier.padding(Spacing.Medium),
                        verticalArrangement = Arrangement.spacedBy(Spacing.ListItemSpacing)
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
                                else -> stringResource(R.string.phantom_ready)
                            },
                            isGood = uiState.shizukuAvailable && uiState.shizukuPermissionGranted,
                            icon = Icons.Default.Security
                        )

                        // Phantom process restriction status
                        StatusRow(
                            label = stringResource(R.string.phantom_current_limit),
                            status = when {
                                uiState.isChecking -> stringResource(R.string.status_loading)
                                uiState.phantomKillerDisabled -> stringResource(R.string.phantom_disabled)
                                else -> stringResource(
                                    R.string.phantom_active,
                                    uiState.currentLimit ?: "32"
                                )
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
                                modifier = Modifier.padding(Spacing.Medium),
                                verticalArrangement = Arrangement.spacedBy(Spacing.ListItemSpacing)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
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
                                            "https://github.com/RikkaApps/Shizuku/releases".toUri()
                                        )
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null)
                                    Spacer(Modifier.width(Spacing.Small))
                                    Text(stringResource(R.string.phantom_shizuku_install))
                                }
                            }
                        }
                    }

                    !uiState.shizukuPermissionGranted -> {
                        // Shizuku permission needed
                        Card {
                            Column(
                                modifier = Modifier.padding(Spacing.Medium),
                                verticalArrangement = Arrangement.spacedBy(Spacing.ListItemSpacing)
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
                                    onClick = { viewModel.requestPermission() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null)
                                    Spacer(Modifier.width(Spacing.Small))
                                    Text(stringResource(R.string.phantom_grant_permission))
                                }
                            }
                        }
                    }

                    !uiState.phantomKillerDisabled -> {
                        // Phantom killer active - offer to disable
                        Card {
                            Column(
                                modifier = Modifier.padding(Spacing.Medium),
                                verticalArrangement = Arrangement.spacedBy(Spacing.ListItemSpacing)
                            ) {
                                Text(
                                    text = stringResource(R.string.phantom_restrictions_active),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = stringResource(R.string.phantom_restrictions_message),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Button(
                                    onClick = { viewModel.disablePhantomKiller() },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !uiState.isProcessing
                                ) {
                                    if (uiState.isProcessing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(IconSize.Small),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Icon(Icons.Default.Block, contentDescription = null)
                                    }
                                    Spacer(Modifier.width(Spacing.Small))
                                    Text(stringResource(R.string.phantom_disable_restrictions))
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
                                modifier = Modifier.padding(Spacing.Medium),
                                verticalArrangement = Arrangement.spacedBy(Spacing.ListItemSpacing)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = stringResource(R.string.phantom_restrictions_disabled),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.phantom_restrictions_disabled_message),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                OutlinedButton(
                                    onClick = {

                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !uiState.isProcessing
                                ) {
                                    if (uiState.isProcessing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(IconSize.Small),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(Icons.Default.Restore, contentDescription = null)
                                    }
                                    Spacer(Modifier.width(Spacing.Small))
                                    Text(stringResource(R.string.phantom_restore_default))
                                }
                            }
                        }
                    }
                }

                // Help section
                Card {
                    Column(
                        modifier = Modifier.padding(Spacing.Medium),
                        verticalArrangement = Arrangement.spacedBy(Spacing.ListItemSpacing)
                    ) {
                        Text(
                            text = stringResource(R.string.phantom_how_to_use),
                            style = MaterialTheme.typography.titleMedium
                        )

                        HorizontalDivider()

                        HelpStep(
                            number = "1",
                            title = stringResource(R.string.phantom_step1_title),
                            description = stringResource(R.string.phantom_step1_desc)
                        )

                        HelpStep(
                            number = "2",
                            title = stringResource(R.string.phantom_step2_title),
                            description = stringResource(R.string.phantom_step2_desc),
                            code = true
                        )

                        HelpStep(
                            number = "3",
                            title = stringResource(R.string.phantom_step3_title),
                            description = stringResource(R.string.phantom_step3_desc)
                        )

                        HelpStep(
                            number = "4",
                            title = stringResource(R.string.phantom_step4_title),
                            description = stringResource(R.string.phantom_step4_desc)
                        )
                    }
                }

                // Alternative methods card
                Card {
                    Column(
                        modifier = Modifier.padding(Spacing.Medium),
                        verticalArrangement = Arrangement.spacedBy(Spacing.ListItemSpacing)
                    ) {
                        Text(
                            text = stringResource(R.string.phantom_alternative_methods),
                            style = MaterialTheme.typography.titleMedium
                        )

                        HorizontalDivider()

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            Text(
                                text = stringResource(R.string.phantom_manual_setting),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.phantom_manual_setting_desc),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(Modifier.height(Spacing.Small))
                        }

                        Text(
                            text = stringResource(R.string.phantom_adb_command),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val adbCommand = stringResource(R.string.adb_shell_command)
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = adbCommand,
                                modifier = Modifier.padding(Spacing.ListItemSpacing),
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