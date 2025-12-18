package com.github.adocker.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.github.adocker.R
import com.github.adocker.ui.components.PullImageDialog
import com.github.adocker.ui.components.StatCard
import com.github.adocker.ui.screens.settings.SettingsViewModel
import com.github.adocker.ui.theme.IconSize
import com.github.adocker.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToMirrorSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val settingsViewModel = hiltViewModel<SettingsViewModel>()
    val homeViewModel = hiltViewModel<HomeViewModel>()
    val stats by homeViewModel.stats.collectAsState()
    val prootVersion by settingsViewModel.prootVersion.collectAsState()

    var showPullDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Stats grid - 2x2网格布局
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.ListItemSpacing)
                ) {
                    StatCard(
                        title = stringResource(R.string.home_images_count),
                        value = "${stats.totalImages}",
                        icon = Icons.Default.Layers,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = stringResource(R.string.home_containers_count),
                        value = "${stats.totalContainers}",
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
                    StatCard(
                        title = stringResource(R.string.home_running_count),
                        value = "${stats.runningContainers}",
                        icon = Icons.Default.PlayCircle,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = stringResource(R.string.home_stopped_count),
                        value = "${stats.stoppedContainers}",
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
                    QuickActionCard(
                        title = stringResource(R.string.home_pull_image),
                        description = stringResource(R.string.home_pull_image_desc),
                        icon = Icons.Default.CloudDownload,
                        onClick = { showPullDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        title = stringResource(R.string.home_mirror_settings),
                        description = stringResource(R.string.home_mirror_settings_desc),
                        icon = Icons.Default.Public,
                        onClick = onNavigateToMirrorSettings,
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
                            value = android.os.Build.SUPPORTED_ABIS.firstOrNull()
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
                                android.os.Build.VERSION.SDK_INT
                            ),
                            icon = Icons.Default.Android
                        )
                    }
                }
            }
        }
    }

    // Pull Image Dialog
    if (showPullDialog) {
        PullImageDialog(
            onDismiss = { showPullDialog = false },
            onNavigateToSearch = {
                showPullDialog = false
                // Navigate to Discover tab would be handled at MainScreen level
            }
        )
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = Spacing.ExtraSmall
        )
    ) {
        Column(
            modifier = Modifier.padding(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            // 图标 - 圆形背景
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                modifier = Modifier.size(IconSize.ExtraLarge)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(IconSize.Large)
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(IconSize.Medium)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
