package com.github.andock.ui.screens.images

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import com.github.andock.R
import com.github.andock.daemon.database.model.ImageEntity
import com.github.andock.daemon.images.ImageReference
import com.github.andock.daemon.images.downloader.ImageDownloader
import com.github.andock.ui.components.LoadingDialog
import com.github.andock.ui.screens.containers.ContainerCreateKey
import com.github.andock.ui.screens.main.LocalNavigator
import com.github.andock.ui.screens.main.LocalResultEventBus
import com.github.andock.ui.screens.qrcode.QrcodeScannerKey
import com.github.andock.ui.screens.qrcode.scannedDataKey
import com.github.andock.ui.theme.IconSize
import com.github.andock.ui.theme.Spacing
import com.github.andock.ui.utils.debounceClick
import com.github.andock.common.withAtLeast
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagesScreen() {
    val navigator = LocalNavigator.current
    val bus = LocalResultEventBus.current
    val viewModel = hiltViewModel<ImagesViewModel>()
    val images by viewModel.images.collectAsState()
    val (showDeleteDialog, setDeleteDialog) = remember { mutableStateOf<ImageEntity?>(null) }
    val (showPullDialog, setPullDialog) = remember { mutableStateOf<Boolean?>(null) }
    val (showProgressDialog, setProgressDialog) = remember { mutableStateOf<ImageDownloader?>(null) }
    val (isLoading, setLoading) = remember { mutableStateOf(false) }
    bus.subscribe(scannedDataKey) { scannedData ->
        if (!scannedData.isNullOrEmpty()) {
            setProgressDialog(
                viewModel.pullImage(
                    ImageReference.parse(scannedData)
                )
            )
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.images_title)) },
                actions = {
                    IconButton(
                        onClick = debounceClick {
                            navigator.navigate(QrcodeScannerKey)
                        }
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = stringResource(R.string.images_scan_qr)
                        )
                    }
                    IconButton(onClick = { setPullDialog(true) }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.images_pull)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (images.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = Spacing.BottomSpacing),
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
                            onClick = { setPullDialog(true) },
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
                            onRun = debounceClick {
                                navigator.navigate(ContainerCreateKey(image.id))
                            },
                            onDelete = debounceClick { setDeleteDialog(image) },
                            onClick = debounceClick {
                                navigator.navigate(ImageDetailKey(image.id))
                            }
                        )
                    }
                    item {
                        Spacer(Modifier.height(Spacing.BottomSpacing))
                    }
                }
            }
        }
    }
    // Delete confirmation dialog
    if (showDeleteDialog != null) {
        ImageDeleteDialog(
            showDeleteDialog,
            onDelete = {
                viewModel.viewModelScope.launch {
                    try {
                        setLoading(true)
                        setDeleteDialog(null)
                        withAtLeast(200) {
                            viewModel.deleteImage(it.id)
                        }
                    } finally {
                        setLoading(false)
                    }
                }
            },
            onDismissRequest = {
                setDeleteDialog(null)
            }
        )
    }
    // Pull Image Dialog
    if (showPullDialog != null) {
        ImagePullDialog(
            onPullImage = {
                setProgressDialog(
                    viewModel.pullImage(
                        ImageReference.parse(it)
                    )
                )
                setPullDialog(null)
            },
            onDismissRequest = {
                setPullDialog(null)
            }
        )
    }
    if (showProgressDialog != null) {
        ImageDownloadDialog(
            downloader = showProgressDialog,
            onDismissRequest = {
                setProgressDialog(null)
            }
        )
    }
    if (isLoading) {
        LoadingDialog()
    }
}
