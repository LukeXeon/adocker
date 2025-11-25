package com.github.adocker.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.adocker.R
import com.github.adocker.core.database.model.ContainerEntity
import com.github.adocker.core.database.model.ContainerStatus

@Composable
fun ContainerCard(
    container: ContainerEntity,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onDelete: () -> Unit,
    onTerminal: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatusIndicator(status = container.status)
                    Column {
                        Text(
                            text = container.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = container.id,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = container.imageName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = container.status.name,
                style = MaterialTheme.typography.labelMedium,
                color = when (container.status) {
                    ContainerStatus.RUNNING -> Color(0xFF4CAF50)
                    ContainerStatus.STOPPED -> Color(0xFFF44336)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (container.status == ContainerStatus.RUNNING) {
                        ActionButton(
                            icon = Icons.Default.Stop,
                            label = stringResource(R.string.action_stop),
                            onClick = onStop
                        )
                        ActionButton(
                            icon = Icons.Default.Terminal,
                            label = stringResource(R.string.action_terminal),
                            onClick = onTerminal
                        )
                    } else {
                        ActionButton(
                            icon = Icons.Default.PlayArrow,
                            label = stringResource(R.string.action_start),
                            onClick = onStart
                        )
                    }
                    ActionButton(
                        icon = Icons.Default.Delete,
                        label = stringResource(R.string.action_delete),
                        onClick = onDelete,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
