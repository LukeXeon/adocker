package com.github.andock.ui.screens.containers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.github.andock.R
import com.github.andock.ui.route.Route
import com.github.andock.ui.screens.main.LocalNavController
import com.github.andock.ui.utils.debounceClick

@OptIn(ExperimentalMaterial3Api::class)
@Route(ContainerLogRoute::class)
@Composable
fun ContainerLogScreen() {
    val viewModel = hiltViewModel<ContainerLogViewModel>()
    val metadata = viewModel.metadata.collectAsState().value ?: return
    val navController = LocalNavController.current
    val logLines = viewModel.logLines.collectAsLazyPagingItems()
    val onNavigateBack = debounceClick {
        navController.popBackStack()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = metadata.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = metadata.id,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E1E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF1E1E1E),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Terminal output
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(
                    logLines.itemCount,
                    logLines.itemKey { it.id }
                ) { index ->
                    val line = logLines[index]
                    if (line != null) {
                        Text(
                            text = line.content,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = getLineColor(line.content),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}