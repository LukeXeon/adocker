package com.github.adocker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.adocker.R
import com.github.adocker.daemon.images.PullProgress
import com.github.adocker.daemon.images.PullStatus

@Composable
fun PullProgressDialog(
    pullProgress: Map<String, PullProgress>,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    val progressList = pullProgress.values.toList()
    val totalLayers = progressList.size
    val completedLayers = progressList.count { it.status == PullStatus.DONE }
    val overallProgress = if (totalLayers > 0) completedLayers.toFloat() / totalLayers else 0f

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.pull_progress_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Overall progress
                Text(
                    text = stringResource(R.string.pull_progress_overall, completedLayers, totalLayers),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { overallProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Layer progress list
                if (progressList.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(progressList) { layerProgress ->
                            LayerProgressItem(layerProgress)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onCancel,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun LayerProgressItem(progress: PullProgress) {
    val layerProgress = if (progress.total > 0) {
        progress.downloaded.toFloat() / progress.total
    } else 0f

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Layer digest (shortened)
        Text(
            text = progress.layerDigest.take(12),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(80.dp)
        )

        // Progress bar
        LinearProgressIndicator(
            progress = { layerProgress },
            modifier = Modifier
                .weight(1f)
                .height(6.dp),
        )

        // Status
        Text(
            text = when (progress.status) {
                PullStatus.WAITING -> "..."
                PullStatus.DOWNLOADING -> "${(layerProgress * 100).toInt()}%"
                PullStatus.DONE -> "OK"
                PullStatus.ERROR -> "ERR"
            },
            style = MaterialTheme.typography.labelSmall,
            color = when (progress.status) {
                PullStatus.DONE -> MaterialTheme.colorScheme.primary
                PullStatus.ERROR -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.width(36.dp)
        )
    }
}
