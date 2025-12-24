package com.github.adocker.ui.screens.registries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.adocker.R
import com.github.adocker.daemon.database.model.RegistryType
import com.github.adocker.daemon.registries.Registry
import com.github.adocker.daemon.registries.RegistryState
import com.github.adocker.ui2.theme.Spacing
import kotlinx.coroutines.flow.map

@Composable
fun RegistryCard(
    registry: Registry,
    onDelete: () -> Unit
) {
    val metadata = registry.metadata.collectAsState().value ?: return
    val isChecking by registry.state.map { it is RegistryState.Checking }.collectAsState(false)
    val isHealthy by registry.state.map { it is RegistryState.Healthy }.collectAsState(false)
    val latencyMs by registry.state.map {
        when (it) {
            is RegistryState.Healthy if it.latencyMs != Long.MAX_VALUE -> {
                "${it.latencyMs}ms"
            }

            is RegistryState.Checking -> {
                "Checking"
            }

            else -> {
                "N/A"
            }
        }
    }.collectAsState("N/A")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Health status icon - show spinner if checking, otherwise show health status
            if (isChecking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = if (isHealthy) "Healthy" else "Unhealthy",
                    tint = if (isHealthy) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                ) {
                    Text(
                        text = metadata.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (metadata.type == RegistryType.CustomMirror) {
                        AssistChip(
                            onClick = { },
                            label = { Text("Custom", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(Spacing.Large),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        )
                    }
                }

                Text(
                    text = metadata.url.removePrefix("https://"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Health details
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.ListItemSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Latency: $latencyMs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    if (metadata.priority > 0) {
                        Text(
                            text = "Priority: ${metadata.priority}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!metadata.bearerToken.isNullOrEmpty()) {
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    "Has Token",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.height(20.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
            if (metadata.type == RegistryType.CustomMirror) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
