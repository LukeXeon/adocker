package com.github.andock.ui.screens.images

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.toRoute
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import com.github.andock.R
import com.github.andock.daemon.images.ImageReference
import com.github.andock.daemon.images.downloader.ImageDownloadState
import com.github.andock.daemon.images.downloader.ImageDownloader
import com.github.andock.ui.components.PaginationColumn
import com.github.andock.ui.components.PaginationEmptyPlaceholder
import com.github.andock.ui.components.PaginationErrorPlaceholder
import com.github.andock.ui.screens.main.LocalNavController
import com.github.andock.ui.theme.Spacing
import com.github.andock.ui.utils.debounceClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageTagSelectScreen() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val repository = remember {
        (lifecycleOwner as? NavBackStackEntry)?.toRoute<ImageTagSelectRoute>()
    }?.repository ?: return
    val isActive = lifecycleOwner.lifecycle.currentStateFlow
        .collectAsState().value == Lifecycle.State.RESUMED
    val viewModel = hiltViewModel<ImagesViewModel>()
    val navController = LocalNavController.current
    val (showProgressDialog, setProgressDialog) = remember { mutableStateOf<ImageDownloader?>(null) }
    val onNavigateBack = debounceClick {
        navController.popBackStack()
    }
    val scope = rememberCoroutineScope()
    val tags = remember { viewModel.tags(repository).cachedIn(scope) }.collectAsLazyPagingItems()
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PaginationColumn(
                tags,
                PaginationEmptyPlaceholder(
                    Icons.Default.Tag,
                    stringResource(R.string.images_tag_empty),
                    stringResource(R.string.images_tag_empty_subtitle)
                ),
                PaginationErrorPlaceholder("Load Failed"),
                { it }
            ) { tagName ->
                Card(
                    enabled = isActive,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        setProgressDialog(
                            viewModel.pullImage(
                                ImageReference.parse(
                                    "$repository:$tagName"
                                )
                            )
                        )
                    },
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(
                                LocalMinimumInteractiveComponentSize.current
                            )
                            .padding(
                                horizontal = Spacing.Medium,
                                vertical = Spacing.Small
                            ),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = tagName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
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