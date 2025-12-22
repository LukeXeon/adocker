package com.github.adocker.ui.screens.mirrors

import androidx.compose.runtime.Composable
import com.github.adocker.daemon.registries.Registry

@Composable
fun MirrorCard(
    registry: Registry,
    onDelete: () -> Unit
) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.surface
//        )
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(Spacing.Medium),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            // Health status icon - show spinner if checking, otherwise show health status
//            if (isChecking) {
//                CircularProgressIndicator(
//                    modifier = Modifier.size(24.dp),
//                    strokeWidth = 2.dp,
//                    color = MaterialTheme.colorScheme.primary
//                )
//            } else {
//                Icon(
//                    imageVector = if (mirror.isHealthy) Icons.Default.CheckCircle else Icons.Default.Error,
//                    contentDescription = if (mirror.isHealthy) "Healthy" else "Unhealthy",
//                    tint = if (mirror.isHealthy) {
//                        MaterialTheme.colorScheme.tertiary
//                    } else {
//                        MaterialTheme.colorScheme.error
//                    }
//                )
//            }
//            Spacer(modifier = Modifier.width(12.dp))
//
//            Column(modifier = Modifier.weight(1f)) {
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
//                ) {
//                    Text(
//                        text = mirror.name,
//                        style = MaterialTheme.typography.titleMedium,
//                        color = MaterialTheme.colorScheme.onSurface
//                    )
//                    if (!mirror.isBuiltIn) {
//                        AssistChip(
//                            onClick = { },
//                            label = { Text("Custom", style = MaterialTheme.typography.labelSmall) },
//                            modifier = Modifier.height(Spacing.Large),
//                            colors = AssistChipDefaults.assistChipColors(
//                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
//                            )
//                        )
//                    }
//                }
//
//                Text(
//                    text = mirror.url.removePrefix("https://"),
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//
//                // Health details
//                Row(
//                    horizontalArrangement = Arrangement.spacedBy(Spacing.ListItemSpacing),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    if (mirror.latencyMs > 0) {
//                        Text(
//                            text = "Latency: ${mirror.latencyMs}ms",
//                            style = MaterialTheme.typography.labelSmall,
//                            color = MaterialTheme.colorScheme.tertiary
//                        )
//                    }
//                    if (mirror.priority > 0) {
//                        Text(
//                            text = "Priority: ${mirror.priority}",
//                            style = MaterialTheme.typography.labelSmall,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
//                    }
//                    if (!mirror.bearerToken.isNullOrEmpty()) {
//                        AssistChip(
//                            onClick = { },
//                            label = {
//                                Text(
//                                    "Has Token",
//                                    style = MaterialTheme.typography.labelSmall
//                                )
//                            },
//                            modifier = Modifier.height(20.dp),
//                            colors = AssistChipDefaults.assistChipColors(
//                                containerColor = MaterialTheme.colorScheme.primaryContainer
//                            )
//                        )
//                    }
//                }
//            }
//            if (onDelete != null) {
//                IconButton(onClick = onDelete) {
//                    Icon(
//                        Icons.Default.Delete,
//                        contentDescription = stringResource(R.string.action_delete),
//                        tint = MaterialTheme.colorScheme.error
//                    )
//                }
//            }
//        }
//    }
}