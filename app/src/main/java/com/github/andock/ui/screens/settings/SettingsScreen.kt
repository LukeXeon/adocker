package com.github.andock.ui.screens.settings

import android.app.usage.StorageStatsManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.storage.StorageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.getSystemService
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import com.github.andock.R
import com.github.andock.daemon.app.AppArchitecture
import com.github.andock.daemon.io.formatFileSize
import com.github.andock.ui.route.Route
import com.github.andock.ui.screens.limits.ProcessLimitRoute
import com.github.andock.ui.screens.main.LocalNavController
import com.github.andock.ui.screens.main.LocalSnackbarHostState
import com.github.andock.ui.screens.registries.RegistriesRoute
import com.github.andock.ui.theme.Spacing
import com.github.andock.ui.utils.debounceClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Route(SettingsRoute::class)
@Composable
fun SettingsScreen() {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val viewModel = hiltViewModel<SettingsViewModel>()
    var storageUsage by remember { mutableStateOf<Long?>(null) }
    val prootVersion by viewModel.prootVersion.collectAsState()
    val snackbarHostState = LocalSnackbarHostState.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }
    LaunchedEffect(Unit) {
        val storageStatsManager = context.getSystemService<StorageStatsManager>()
        val storageManager = context.getSystemService<StorageManager>()
        if (storageStatsManager != null && storageManager != null) {
            suspend {
                val storageVolume = storageManager.primaryStorageVolume
                val uuid = storageVolume.uuid?.let { UUID.fromString(it) }
                    ?: StorageManager.UUID_DEFAULT
                withContext(Dispatchers.IO) {
                    while (isActive) {
                        val storageStats = storageStatsManager.queryStatsForPackage(
                            uuid,
                            context.packageName,
                            UserHandle.getUserHandleForUid(Process.myUid())
                        )
                        storageUsage = storageStats.dataBytes
                        delay(1000)
                    }
                }
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) }
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Network section
            item {
                SettingsSection(title = stringResource(R.string.settings_network)) {
                    SettingsClickableItem(
                        icon = Icons.Default.Cloud,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = stringResource(R.string.settings_registry_mirror),
                        subtitle = stringResource(R.string.settings_registry_mirror_subtitle),
                        onClick = debounceClick {
                            navController.navigate(RegistriesRoute)
                        }
                    )
                }
            }

            // System section
            item {
                SettingsSection(title = stringResource(R.string.settings_system)) {
                    SettingsClickableItem(
                        icon = Icons.Default.Block,
                        iconTint = MaterialTheme.colorScheme.error,
                        title = stringResource(R.string.settings_phantom_process),
                        subtitle = stringResource(R.string.settings_phantom_process_subtitle),
                        onClick = debounceClick {
                            navController.navigate(ProcessLimitRoute)
                        },
                        isWarning = true
                    )
                    SettingsItem(
                        icon = Icons.Default.PhoneAndroid,
                        title = stringResource(R.string.settings_platform),
                        subtitle = stringResource(
                            R.string.settings_platform_value,
                            Build.VERSION.RELEASE,
                            Build.VERSION.SDK_INT
                        )
                    )

                    SettingsItem(
                        icon = Icons.Default.Memory,
                        title = stringResource(R.string.settings_architecture),
                        subtitle = AppArchitecture.DEFAULT
                    )
                }
            }

            // Storage section
            item {
                SettingsSection(title = stringResource(R.string.settings_storage)) {
                    SettingsItem(
                        icon = Icons.Default.Storage,
                        title = stringResource(R.string.settings_storage_usage),
                        subtitle = storageUsage?.let { formatFileSize(it) }
                            ?: stringResource(R.string.status_loading)
                    )

                    SettingsClickableItem(
                        icon = Icons.Default.Settings,
                        title = stringResource(R.string.settings_manage_data),
                        subtitle = stringResource(R.string.settings_mange_data_subtitle),
                        onClick = {
                            launcher.launch(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts(
                                        "package",
                                        context.packageName,
                                        null
                                    )
                                )
                            )
                        }
                    )
                }
            }

            // About section
            item {
                SettingsSection(title = stringResource(R.string.settings_about)) {
                    SettingsClickableItem(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.settings_version),
                        subtitle = viewModel.packageInfo.versionName ?: "",
                        onClick = {
                            viewModel.viewModelScope.launch {
                                snackbarHostState.showSnackbar(
                                    viewModel.packageInfo.versionName ?: ""
                                )
                            }
                        }
                    )

                    SettingsItem(
                        icon = Icons.Default.Terminal,
                        title = stringResource(R.string.settings_engine),
                        subtitle = prootVersion ?: stringResource(R.string.terminal_unavailable)
                    )
                }
            }

            // 呼吸空间
            item {
                Spacer(Modifier.height(Spacing.BottomSpacing))
            }
        }
    }
}