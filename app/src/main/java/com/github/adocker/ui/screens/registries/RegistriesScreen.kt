package com.github.adocker.ui.screens.registries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.github.adocker.R
import com.github.adocker.daemon.app.AppGlobals
import com.github.adocker.ui.screens.main.Screen
import com.github.adocker.ui.screens.qrcode.MirrorQRCode
import com.github.adocker.ui.theme.Spacing
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistriesScreen(
    navController: NavHostController,
    scannedMirrorData: String? = null,
) {
    val viewModel = hiltViewModel<RegistriesViewModel>()
    val json = AppGlobals.json()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val mirrors by viewModel.mirrors.map { it.values.toList() }.collectAsState(emptyList())

    // Handle scanned mirror data
    LaunchedEffect(scannedMirrorData) {
        if (scannedMirrorData != null) {
            try {
                val mirrorQRCode = json.runCatching {
                    decodeFromString<MirrorQRCode>(scannedMirrorData)
                }.getOrNull()
                if (mirrorQRCode != null) {
                    viewModel.addCustomMirror(
                        mirrorQRCode.name,
                        mirrorQRCode.url,
                        mirrorQRCode.bearerToken
                    )
                    snackbarHostState.showSnackbar("Mirror imported: ${mirrorQRCode.name}")
                } else {
                    snackbarHostState.showSnackbar("Invalid QR code format")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Failed to import mirror: ${e.message}")
            }
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var mirrorToDelete by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.mirror_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.checkMirrorsNow() }
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Check Health"
                        )
                    }
                    IconButton(
                        onClick = {
                            navController.navigate(Screen.QRCodeScanner.route)
                        }
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = stringResource(R.string.mirror_settings_scan_qr)
                        )
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.mirror_settings_add)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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

            items(mirrors, key = { it.id }) { mirror ->
                MirrorCard(
                    registry = mirror,
                    onDelete = { mirrorToDelete = mirror.id }
                )
            }

            item {
                Spacer(modifier = Modifier.height(Spacing.Medium))
                OutlinedCard(
                    onClick = { showAddDialog = true },
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

    // Add mirror dialog
    if (showAddDialog) {
        AddMirrorDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, url, token ->
                viewModel.addCustomMirror(name, url, token)
                showAddDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Mirror added: $name")
                }
            }
        )
    }

    // Delete confirmation dialog
//    mirrorToDelete?.let { mirror ->
//        AlertDialog(
//            onDismissRequest = { mirrorToDelete = null },
//            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
//            title = { Text(stringResource(R.string.mirror_settings_delete_title)) },
//            text = { Text(stringResource(R.string.mirror_settings_delete_message, mirror.name)) },
//            confirmButton = {
//                TextButton(
//                    onClick = {
//                        viewModel.deleteCustomMirror(mirror)
//                        mirrorToDelete = null
//                        scope.launch {
//                            snackbarHostState.showSnackbar("Mirror deleted")
//                        }
//                    },
//                    colors = ButtonDefaults.textButtonColors(
//                        contentColor = MaterialTheme.colorScheme.error
//                    )
//                ) {
//                    Text(stringResource(R.string.action_delete))
//                }
//            },
//            dismissButton = {
//                TextButton(onClick = { mirrorToDelete = null }) {
//                    Text(stringResource(R.string.action_cancel))
//                }
//            }
//        )
//    }
}