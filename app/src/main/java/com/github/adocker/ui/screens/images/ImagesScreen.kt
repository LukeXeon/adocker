package com.github.adocker.ui.screens.images

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.adocker.R
import com.github.adocker.ui.theme.IconSize
import com.github.adocker.ui.theme.Spacing
import com.github.adocker.daemon.database.model.ImageEntity
import com.github.adocker.ui.components.ImageCard
import com.github.adocker.ui.components.PullImageDialog
import com.github.adocker.ui.components.PullProgressDialog
import com.github.adocker.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagesScreen(
    viewModel: MainViewModel,
    onNavigateToCreate: (String) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToQRScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val images by viewModel.images.collectAsState()
    val pullProgress by viewModel.pullProgress.collectAsState()
    val isPulling by viewModel.isPulling.collectAsState()

    var showDeleteDialog by remember { mutableStateOf<ImageEntity?>(null) }
    var showPullDialog by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }

    // When pulling starts, switch to progress dialog
    LaunchedEffect(isPulling) {
        if (isPulling && showPullDialog) {
            showPullDialog = false
            showProgressDialog = true
        }
    }

    // When pulling completes, close progress dialog
    LaunchedEffect(isPulling, pullProgress) {
        if (!isPulling && showProgressDialog && pullProgress.isEmpty()) {
            showProgressDialog = false
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.images_title)) },
            actions = {
                IconButton(onClick = onNavigateToQRScanner) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = stringResource(R.string.images_scan_qr)
                    )
                }
                IconButton(onClick = { showPullDialog = true }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.images_pull)
                    )
                }
            }
        )

        if (images.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(Spacing.Large)
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.Huge),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(Spacing.Large))
                    Text(
                        text = stringResource(R.string.images_empty),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(Spacing.Small))
                    Text(
                        text = stringResource(R.string.images_empty_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(Spacing.Large))
                    FilledTonalButton(
                        onClick = { showPullDialog = true },
                        contentPadding = PaddingValues(
                            horizontal = Spacing.Large,
                            vertical = Spacing.Medium
                        )
                    ) {
                        Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(IconSize.Small)
                        )
                        Spacer(modifier = Modifier.width(Spacing.Small))
                        Text(stringResource(R.string.images_pull))
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Spacing.ScreenPadding,
                    top = Spacing.Medium,
                    end = Spacing.ScreenPadding,
                    bottom = Spacing.Medium
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.ListItemSpacing)
            ) {
                items(images, key = { it.id }) { image ->
                    ImageCard(
                        image = image,
                        onRun = { onNavigateToCreate(image.id) },
                        onDelete = { showDeleteDialog = image },
                        onClick = { onNavigateToDetail(image.id) }
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { image ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text(stringResource(R.string.images_delete_confirm_title)) },
            text = {
                Text(stringResource(R.string.images_delete_confirm_message, image.fullName))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteImage(image.id)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Pull Image Dialog
    if (showPullDialog) {
        PullImageDialog(
            viewModel = viewModel,
            onDismiss = { showPullDialog = false },
            onNavigateToSearch = { showPullDialog = false }
        )
    }

    // Pull Progress Dialog
    if (showProgressDialog) {
        PullProgressDialog(
            pullProgress = pullProgress,
            onCancel = {
                // Close dialog but continue pulling in background
                showProgressDialog = false
            },
            onDismiss = {
                if (!isPulling) {
                    showProgressDialog = false
                }
            }
        )
    }
}
