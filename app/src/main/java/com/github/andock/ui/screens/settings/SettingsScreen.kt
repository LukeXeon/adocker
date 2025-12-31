package com.github.andock.ui.screens.settings

import android.os.Build
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
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import com.github.andock.R
import com.github.andock.daemon.app.AppContext
import com.github.andock.daemon.io.formatFileSize
import com.github.andock.ui.components.LoadingDialog
import com.github.andock.ui.screens.limits.ProcessLimitRoute
import com.github.andock.ui.screens.main.LocalNavController
import com.github.andock.ui.screens.main.LocalSnackbarHostState
import com.github.andock.ui.screens.registries.RegistriesRoute
import com.github.andock.ui.theme.Spacing
import com.github.andock.ui.utils.debounceClick
import com.github.andock.ui.utils.withAtLeast
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val navController = LocalNavController.current
    val viewModel = hiltViewModel<SettingsViewModel>()
    val snackbarHostState = LocalSnackbarHostState.current
    val storageUsage by viewModel.storageUsage.collectAsState()
    val (isLoading, setLoading) = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.loadStorageUsage()
    }
    val prootVersion by viewModel.prootVersion.collectAsState()
    val (isShowClearDataDialog, setShowDataDialogState) = remember { mutableStateOf(false) }
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
                    icon = Icons.Default.DeleteSweep,
                    iconTint = MaterialTheme.colorScheme.error,
                    title = stringResource(R.string.settings_clear_data),
                    subtitle = stringResource(R.string.settings_clear_data_subtitle),
                    onClick = {
                        setShowDataDialogState(true)
                    },
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
    if (isShowClearDataDialog) {
        val clearSuccessMessage = stringResource(R.string.settings_clear_data_success)
        SettingsClearDataDialog(
            onClear = {
                viewModel.viewModelScope.launch {
                    try {
                        setLoading(true)
                        setShowDataDialogState(false)
                        withAtLeast(200) {
                            viewModel.clearAllData()
                        }
                    } finally {
                        setLoading(false)
                    }
                }
            },
            onDismissRequest = {
                setShowDataDialogState(false)
                viewModel.viewModelScope.launch {
                    snackbarHostState.showSnackbar(clearSuccessMessage)
                }
            }
        )
    }
    if (isLoading) {
        LoadingDialog()
    }

}