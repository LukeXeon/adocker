package com.github.adocker.ui2.screens.containers

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.github.adocker.R
import com.github.adocker.daemon.containers.Container
import com.github.adocker.daemon.containers.ContainerState
import com.github.adocker.daemon.database.model.ContainerEntity
import com.github.adocker.ui2.components.StatusIndicator
import com.github.adocker.ui.theme.IconSize
import com.github.adocker.ui.theme.Spacing

/**
 * Material Design 3 容器卡片组件
 * 可展开的容器信息卡片,显示容器名称、状态、镜像等信息
 * 展开后根据状态显示对应操作按钮
 */

@Composable
fun ContainerCard(
    container: Container,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onDelete: () -> Unit,
    onTerminal: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var containerInfo by remember { mutableStateOf<ContainerEntity?>(null) }
    // Observe container state changes in real-time
    val containerState by container.state.collectAsState()

    LaunchedEffect(container) {
        container.getMetadata().onSuccess { entity ->
            containerInfo = entity
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(Spacing.Medium)
        ) {
            // 标题行: 状态指示器 + 名称/ID + 展开按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
                    modifier = Modifier.weight(1f)
                ) {
                    // 容器图标 + 状态指示器
                    Box {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = when (containerState) {
                                is ContainerState.Running -> MaterialTheme.colorScheme.tertiaryContainer
                                is ContainerState.Created,
                                is ContainerState.Starting -> MaterialTheme.colorScheme.primaryContainer

                                else -> MaterialTheme.colorScheme.errorContainer
                            },
                            modifier = Modifier.size(IconSize.Large)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ViewInAr,
                                    contentDescription = null,
                                    tint = when (containerState) {
                                        is ContainerState.Running -> MaterialTheme.colorScheme.onTertiaryContainer
                                        is ContainerState.Created,
                                        is ContainerState.Starting -> MaterialTheme.colorScheme.onPrimaryContainer

                                        else -> MaterialTheme.colorScheme.onErrorContainer
                                    },
                                    modifier = Modifier.size(IconSize.Medium)
                                )
                            }
                        }
                        // 右下角状态指示点
                        StatusIndicator(
                            state = containerState,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = Spacing.ExtraSmall, y = Spacing.ExtraSmall)
                        )
                    }

                    // 名称和ID
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = containerInfo?.name ?: container.id.take(12),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                        Text(
                            text = container.id.take(12),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // 展开按钮
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.Medium))

            // 镜像名称和状态标签
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(IconSize.Small)
                )
                Text(
                    text = containerInfo?.imageName ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(Spacing.Small))

            // 状态标签 - 显示ContainerState子类的简单类名
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = when (containerState) {
                    is ContainerState.Running -> MaterialTheme.colorScheme.tertiaryContainer
                    is ContainerState.Created,
                    is ContainerState.Starting -> MaterialTheme.colorScheme.secondaryContainer

                    else -> MaterialTheme.colorScheme.errorContainer
                }
            ) {
                Text(
                    text = containerState::class.simpleName ?: "Unknown",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = when (containerState) {
                        is ContainerState.Running -> MaterialTheme.colorScheme.onTertiaryContainer
                        is ContainerState.Created,
                        is ContainerState.Starting -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onErrorContainer
                    },
                    modifier = Modifier.padding(
                        horizontal = Spacing.Small,
                        vertical = Spacing.ExtraSmall
                    )
                )
            }

            // 展开内容: 操作按钮
            if (expanded) {
                Spacer(modifier = Modifier.height(Spacing.Medium))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(modifier = Modifier.height(Spacing.Medium))

                // 操作按钮
                if (containerState is ContainerState.Running) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                    ) {
                        FilledTonalButton(
                            onClick = onTerminal,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(Spacing.Medium)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                modifier = Modifier.size(IconSize.Small)
                            )
                            Spacer(modifier = Modifier.width(Spacing.Small))
                            Text(stringResource(R.string.action_terminal))
                        }
                        OutlinedButton(
                            onClick = onStop,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(Spacing.Medium)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(IconSize.Small)
                            )
                            Spacer(modifier = Modifier.width(Spacing.Small))
                            Text(stringResource(R.string.action_stop))
                        }
                    }
                } else {
                    FilledTonalButton(
                        onClick = onStart,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(Spacing.Medium)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(IconSize.Small)
                        )
                        Spacer(modifier = Modifier.width(Spacing.Small))
                        Text(stringResource(R.string.action_start))
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.Small))

                // 删除按钮 - 单独一行,危险操作
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    contentPadding = PaddingValues(Spacing.Medium)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.Small)
                    )
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    Text(stringResource(R.string.action_delete))
                }
            }
        }
    }
}
