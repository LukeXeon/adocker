package com.github.andock.ui.screens.images

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.github.andock.R
import com.github.andock.daemon.images.ImageReference
import com.github.andock.daemon.images.downloader.ImageDownloadState
import com.github.andock.daemon.images.downloader.ImageDownloader
import com.github.andock.ui.components.InfoCard
import com.github.andock.ui.components.PaginationColumn
import com.github.andock.ui.components.PaginationPlaceholder
import com.github.andock.ui.route.Route
import com.github.andock.ui.screens.main.LocalNavController
import com.github.andock.ui.theme.Spacing
import com.github.andock.ui.utils.debounceClick

@OptIn(ExperimentalMaterial3Api::class)
@Route(ImageTagsRoute::class)
@Composable
fun ImageTagsScreen() {
    val viewModel = hiltViewModel<ImageTagsViewModel>()
    val repository = viewModel.repository
    val navController = LocalNavController.current
    val (showProgressDialog, setProgressDialog) = remember { mutableStateOf<ImageDownloader?>(null) }
    val onNavigateBack = debounceClick {
        navController.popBackStack()
    }
    val tags = viewModel.tags.collectAsLazyPagingItems()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.images_tag)) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
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
            Box(
                modifier = Modifier.padding(Spacing.Medium),
            ) {
                InfoCard(
                    imageVector = Icons.Default.Layers,
                    title = repository,
                    subtitle = "select one tag download"
                )
            }
            PaginationColumn(
                items = tags,
                itemKey = { it },
                itemContent = {
                    ImageTagCard(it) {
                        setProgressDialog(
                            viewModel.pullImage(
                                ImageReference.parse(
                                    "$repository:$it"
                                )
                            )
                        )
                    }
                },
                initialContent = {
                    PaginationPlaceholder.Initial()
                },
                emptyContent = {
                    PaginationPlaceholder.Empty(
                        Icons.Default.Tag,
                        stringResource(R.string.images_tag_empty),
                        stringResource(R.string.images_tag_empty_subtitle)
                    )
                },
                errorContent = {
                    PaginationPlaceholder.Error(it, "Load Failed") {
                        tags.retry()
                    }
                }
            )
        }
    }
    if (showProgressDialog != null) {
        ImageDownloadDialog(
            downloader = showProgressDialog,
            onDismissRequest = {
                setProgressDialog(null)
                if (showProgressDialog.state.value is ImageDownloadState.Done) {
                    onNavigateBack()
                }
            }
        )
    }
}