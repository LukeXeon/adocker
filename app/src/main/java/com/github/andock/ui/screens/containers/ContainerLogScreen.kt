package com.github.andock.ui.screens.containers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.github.andock.R
import com.github.andock.ui.components.InfoCard
import com.github.andock.ui.components.PaginationColumn
import com.github.andock.ui.components.PaginationPlaceholder
import com.github.andock.ui.route.Route
import com.github.andock.ui.screens.main.LocalNavController
import com.github.andock.ui.utils.debounceClick

@OptIn(ExperimentalMaterial3Api::class)
@Route(ContainerLogRoute::class)
@Composable
fun ContainerLogScreen() {
    val viewModel = hiltViewModel<ContainerLogViewModel>()
    val containerId = viewModel.containerId
    val navController = LocalNavController.current
    val name = viewModel.container.collectAsState()
        .value?.metadata?.collectAsState()?.value?.name ?: ""
    val logLines = viewModel.logLines.collectAsLazyPagingItems()
    val onNavigateBack = debounceClick {
        navController.popBackStack()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.log_title)) },
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
            InfoCard(
                imageVector = Icons.Filled.ViewInAr,
                title = name,
                subtitle = containerId.take(12)
            )
            PaginationColumn(
                items = logLines,
                itemKey = { it.id },
                itemContent = {
                    Text(it.message)
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
                        logLines.retry()
                    }
                }
            )
        }
    }
}