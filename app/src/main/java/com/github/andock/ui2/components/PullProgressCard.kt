package com.github.andock.ui2.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.andock.R
import com.github.andock.daemon.images.PullProgress

@Composable
fun PullProgressCard(
    imageName: String,
    progress: Map<String, PullProgress>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.pull_image_downloading, imageName),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            progress.values.forEach { layerProgress ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = layerProgress.layerDigest.take(12),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(0.3f)
                    )

                    LinearProgressIndicator(
                        progress = {
                            if (layerProgress.total > 0) {
                                (layerProgress.downloaded.toFloat() / layerProgress.total)
                            } else 0f
                        },
                        modifier = Modifier
                            .weight(0.5f)
                            .height(8.dp),
                    )

                    Text(
                        text = layerProgress.status.name,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(0.2f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
