package com.github.andock.ui.screens.images

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
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
import com.github.andock.R
import com.github.andock.daemon.database.model.ImageEntity
import com.github.andock.daemon.io.formatFileSize
import com.github.andock.ui.theme.IconSize
import com.github.andock.ui.theme.Spacing
import com.github.andock.ui.components.InfoChip

/**
 * Material Design 3 镜像卡片组件
 * 可展开的镜像信息卡片,显示镜像名称、标签、大小、架构等信息
 * 展开后显示完整ID和操作按钮
 */
@Composable
fun ImageCard(
    image: ImageEntity,
    onRun: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = Spacing.ExtraSmall
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(Spacing.Medium)
        ) {
            // 标题行: 图标 + 名称/标签 + 展开按钮
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
                    // 镜像图标 - 带背景圆形
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(IconSize.Large)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(IconSize.Medium)
                            )
                        }
                    }

                    // 名称和标签
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = image.repository.removePrefix("library/"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = image.tag,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(
                                    horizontal = Spacing.Small,
                                    vertical = Spacing.ExtraSmall
                                )
                            )
                        }
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

            // 信息标签行: 大小 + 架构
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                InfoChip(
                    label = formatFileSize(image.size),
                    icon = Icons.Default.Storage
                )
                InfoChip(
                    label = image.architecture,
                    icon = Icons.Default.Memory
                )
            }

            // 展开内容: 完整ID + 操作按钮
            if (expanded) {
                Spacer(modifier = Modifier.height(Spacing.Medium))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(modifier = Modifier.height(Spacing.Medium))

                // 镜像ID
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(IconSize.Small)
                    )
                    Text(
                        text = "ID: ${image.id.take(12)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.Medium))

                // 操作按钮行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                ) {
                    // 运行按钮 - 填充样式
                    FilledTonalButton(
                        onClick = onRun,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(Spacing.Medium)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(IconSize.Small)
                        )
                        Spacer(modifier = Modifier.width(Spacing.Small))
                        Text(stringResource(R.string.action_run))
                    }

                    // 删除按钮 - 轮廓样式
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
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
}
