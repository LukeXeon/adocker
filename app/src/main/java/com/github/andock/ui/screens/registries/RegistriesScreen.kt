package com.github.andock.ui.screens.registries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavBackStackEntry
import com.github.andock.R
import com.github.andock.daemon.registries.Registry
import com.github.andock.ui.screens.main.LocalNavController
import com.github.andock.ui.screens.main.LocalSnackbarHostState
import com.github.andock.ui.screens.qrcode.QrcodeScannerRoute
import com.github.andock.ui.screens.qrcode.ScannedData
import com.github.andock.ui.theme.Spacing
import com.github.andock.ui.utils.debounceClick
import com.github.andock.ui.utils.get
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistriesScreen() {
    val navController = LocalNavController.current
    val viewModel = hiltViewModel<RegistriesViewModel>()
    val snackbarHostState = LocalSnackbarHostState.current
    val registries by viewModel.sortedList.collectAsState()
    val bestServer by viewModel.bestServer.collectAsState()
    val (serverToDelete, setServerToDelete) = remember { mutableStateOf<Registry?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    if (lifecycleOwner is NavBackStackEntry) {
        val scannedDataFlow = lifecycleOwner.savedStateHandle[ScannedData]
        val scannedData = scannedDataFlow.collectAsState().value
        if (!scannedData.isNullOrEmpty()) {
            val message = stringResource(R.string.message_error)
            LaunchedEffect(scannedData) {
                if (!viewModel.addScannedCode(scannedData)) {
                    snackbarHostState.showSnackbar(message)
                }
            }
        }
    }
    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.mirror_settings_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = debounceClick {
                            navController.popBackStack()
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.checkAll() }
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Check Health"
                        )
                    }
                    IconButton(
                        onClick = debounceClick {
                            navController.navigate(QrcodeScannerRoute)
                        },
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = stringResource(R.string.mirror_settings_scan_qr)
                        )
                    }
                }
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                horizontal = Spacing.ScreenPadding,
                vertical = Spacing.Medium
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            item {
                Text(
                    text = stringResource(R.string.mirror_settings_auto_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(registries, key = { it.id }) { server ->
                RegistryCard(
                    registry = server,
                    isBest = server == bestServer,
                    onDelete = { setServerToDelete(server) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(Spacing.Medium))
                OutlinedCard(
                    onClick = debounceClick {
                        navController.navigate(AddMirrorRoute)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.Medium),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(Spacing.Small))
                        Text(
                            stringResource(R.string.mirror_settings_add),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
    if (serverToDelete != null) {
        RegistryDeleteDialog(
            serverToDelete,
            onDelete = { registry ->
                viewModel.viewModelScope.launch {
                    viewModel.deleteCustomMirror(registry.id)
                    snackbarHostState.showSnackbar("Mirror deleted")
                }
            },
            onDismissRequest = {
                setServerToDelete(null)
            }
        )
    }
}