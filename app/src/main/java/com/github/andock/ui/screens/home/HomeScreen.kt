package com.github.andock.ui.screens.home

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.github.andock.R
import com.github.andock.daemon.images.ImageReference
import com.github.andock.daemon.images.downloader.ImageDownloader
import com.github.andock.ui.components.BottomSpacer
import com.github.andock.ui.components.InfoRow
import com.github.andock.ui.screens.images.ImageDownloadDialog
import com.github.andock.ui.screens.images.ImagePullDialog
import com.github.andock.ui.screens.limits.ProcessLimitWarningDialog
import com.github.andock.ui.screens.main.LocalNavController
import com.github.andock.ui.screens.registries.RegistriesRoute
import com.github.andock.ui.theme.IconSize
import com.github.andock.ui.theme.Spacing
import com.github.andock.ui.utils.debounceClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val navController = LocalNavController.current
    val viewModel = hiltViewModel<HomeViewModel>()
    val totalImages by viewModel.totalImages.collectAsState(0)
    val totalContainers by viewModel.totalContainers.collectAsState(0)
    val runningContainers by viewModel.runningContainers.collectAsState(0)
    val stoppedContainers by viewModel.stoppedContainers.collectAsState(0)
    val isShowWarning by viewModel.isShowWarning.collectAsState()
    val prootVersion by viewModel.prootVersion.collectAsState()
    val (showPullDialog, setPullDialog) = remember { mutableStateOf<Boolean?>(null) }
    val (showProgressDialog, setProgressDialog) = remember { mutableStateOf<ImageDownloader?>(null) }

    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(stringResource(R.string.app_name))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = Spacing.ScreenPadding,
                top = Spacing.Medium,
                end = Spacing.ScreenPadding,
                bottom = Spacing.Medium
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.ContentSpacing)
        ) {
            // Welcome card - 突出的欢迎卡片
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = Spacing.Small
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.Large)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Rocket,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(IconSize.Large)
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.home_welcome),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                                Text(
                                    text = stringResource(R.string.home_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // Overview section
            item {
                Text(
                    text = stringResource(R.string.home_overview),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Stats grid - 2x2网格布局
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.ListItemSpacing)
                ) {
                    HomeStatCard(
                        title = stringResource(R.string.home_images_count),
                        value = "$totalImages",
                        icon = Icons.Default.Layers,
                        modifier = Modifier.weight(1f)
                    )
                    HomeStatCard(
                        title = stringResource(R.string.home_containers_count),
                        value = "$totalContainers",
                        icon = Icons.Default.ViewInAr,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.ListItemSpacing)
                ) {
                    HomeStatCard(
                        title = stringResource(R.string.home_running_count),
                        value = "$runningContainers",
                        icon = Icons.Default.PlayCircle,
                        modifier = Modifier.weight(1f)
                    )
                    HomeStatCard(
                        title = stringResource(R.string.home_stopped_count),
                        value = "$stoppedContainers",
                        icon = Icons.Default.StopCircle,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick actions section
            item {
                Spacer(modifier = Modifier.height(Spacing.Small))
                Text(
                    text = stringResource(R.string.home_quick_actions),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.ListItemSpacing)
                ) {
                    HomeQuickActionCard(
                        title = stringResource(R.string.home_pull_image),
                        description = stringResource(R.string.home_pull_image_desc),
                        icon = Icons.Default.CloudDownload,
                        onClick = { setPullDialog(true) },
                        modifier = Modifier.weight(1f)
                    )
                    HomeQuickActionCard(
                        title = stringResource(R.string.home_mirror_settings),
                        description = stringResource(R.string.home_mirror_settings_desc),
                        icon = Icons.Default.Public,
                        onClick = debounceClick {
                            navController.navigate(RegistriesRoute)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // About section
            item {
                Spacer(modifier = Modifier.height(Spacing.Small))
                Text(
                    text = stringResource(R.string.home_about),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.Medium),
                        verticalArrangement = Arrangement.spacedBy(Spacing.Small)
                    ) {
                        InfoRow(
                            label = stringResource(R.string.home_engine),
                            value = prootVersion
                                ?: stringResource(R.string.terminal_unavailable),
                            icon = Icons.Default.Settings
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = Spacing.ExtraSmall)
                        )
                        InfoRow(
                            label = stringResource(R.string.home_architecture),
                            value = Build.SUPPORTED_ABIS.firstOrNull()
                                ?: stringResource(R.string.home_unknown),
                            icon = Icons.Default.Memory
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = Spacing.ExtraSmall)
                        )
                        InfoRow(
                            label = stringResource(R.string.home_android_version),
                            value = stringResource(
                                R.string.home_api_level,
                                Build.VERSION.SDK_INT
                            ),
                            icon = Icons.Default.Android
                        )
                    }
                }
            }

            item {
                BottomSpacer()
            }
        }
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
    if (isShowWarning == true) {
        ProcessLimitWarningDialog(
            onDismissRequest = {
                viewModel.dismissWarning()
            }
        )
    }
}