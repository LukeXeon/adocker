package com.github.andock.ui.screens.limits

import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.github.andock.R
import com.github.andock.ui.components.HelpStep
import com.github.andock.ui.components.StatusRow
import com.github.andock.ui.screens.main.LocalSnackbarHostState
import com.github.andock.ui.theme.IconSize
import com.github.andock.ui.theme.Spacing
import kotlinx.coroutines.launch

@Composable
fun ProcessLimited() {
    val snackbarHostState = LocalSnackbarHostState.current
    val viewModel = hiltViewModel<ProcessLimitedViewModel>()
    val stats by viewModel.stats.collectAsState()
    val (isProcessing, setProcessing) = remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.scheduleRefresh()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
    ) {
        // Status overview card
        item {
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
                            !stats.isAvailable -> stringResource(R.string.phantom_shizuku_not_running)
                            !stats.hasPermission -> stringResource(R.string.phantom_permission_denied)
                            else -> stringResource(R.string.phantom_ready)
                        },
                        isGood = stats.isAvailable && stats.hasPermission,
                        icon = Icons.Default.Security
                    )

                    // Phantom process restriction status
                    StatusRow(
                        label = stringResource(R.string.phantom_current_limit),
                        status = when {
                            stats.isUnrestricted -> stringResource(R.string.phantom_disabled)
                            else -> stringResource(
                                R.string.phantom_active,
                                stats.currentLimit
                            )
                        },
                        isGood = stats.isUnrestricted,
                        icon = Icons.Default.Block
                    )
                }
            }
        }
        // Action buttons
        when {
            !stats.isAvailable -> {
                // Shizuku not installed
                item {
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
                                    launcher.launch(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            "https://github.com/RikkaApps/Shizuku/releases".toUri()
                                        )
                                    )
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
            }

            !stats.hasPermission -> {
                // Shizuku permission needed
                item {
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
            }

            !stats.isUnrestricted -> {
                // Phantom killer active - offer to disable
                item {
                    val successMessage = stringResource(R.string.phantom_restrictions_disabled)
                    val errorMessage = stringResource(R.string.message_error)
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
                                onClick = {
                                    viewModel.viewModelScope.launch {
                                        setProcessing(true)
                                        val success = viewModel.unrestrict()
                                        setProcessing(false)
                                        snackbarHostState.showSnackbar(
                                            if (success) {
                                                successMessage
                                            } else {
                                                errorMessage
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isProcessing
                            ) {
                                if (isProcessing) {
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
            }

            else -> {
                // Phantom killer disabled - success state
                item {
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
                                enabled = !isProcessing
                            ) {
                                if (isProcessing) {
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
        }

        // Help section
        item {
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
        }

        // Alternative methods card
        item {
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