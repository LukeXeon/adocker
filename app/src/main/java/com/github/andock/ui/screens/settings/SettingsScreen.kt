package com.github.andock.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.github.andock.R
import com.github.andock.daemon.app.AppContext
import com.github.andock.daemon.io.formatFileSize
import com.github.andock.ui.screens.limits.ProcessLimitRoute
import com.github.andock.ui.screens.main.LocalNavController
import com.github.andock.ui.screens.registries.RegistriesRoute
import com.github.andock.ui.theme.Spacing
import com.github.andock.ui.utils.debounceClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val navController = LocalNavController.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel = hiltViewModel<SettingsViewModel>()
    val storageUsage by viewModel.storageUsage.collectAsState()
    val prootVersion by viewModel.prootVersion.collectAsState()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }
    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.loadStorageUsage()
        }
    }
    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) }
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Network section
            SettingsSection(title = stringResource(R.string.settings_network)) {
                SettingsClickableItem(
                    icon = Icons.Default.Cloud,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = stringResource(R.string.settings_registry_mirror),
                    subtitle = stringResource(R.string.settings_registry_mirror_subtitle),
                    onClick = debounceClick {
                        navController.navigate(RegistriesRoute())
                    }
                )
            }

            // System section
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
}