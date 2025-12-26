package com.github.andock.ui.screens.images

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.andock.R
import com.github.andock.daemon.images.ImageDownloadState
import com.github.andock.daemon.images.ImageDownloader

@Composable
fun ImageDownloadDialog(
    downloader: ImageDownloader,
    onDismissRequest: () -> Unit
) {
    val imageName = downloader.ref.fullName
    val state = downloader.state.collectAsState().value
    AlertDialog(
        onDismissRequest = { },
        icon = { Icon(Icons.Default.Download, contentDescription = null) },
        title = { Text(stringResource(R.string.pull_image_title)) },
        text = {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.pull_image_downloading, imageName),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    when (state) {
                        is ImageDownloadState.Downloading -> {
                            val progress by state.progress.collectAsState()
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                items(progress.entries.toList(), { it.key }) { (id, progress) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = id.take(12),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.weight(0.3f)
                                        )

                                        LinearProgressIndicator(
                                            progress = {
                                                if (progress.total > 0) {
                                                    (progress.downloaded.toFloat() / progress.total)
                                                } else 0f
                                            },
                                            modifier = Modifier
                                                .weight(0.5f)
                                                .height(8.dp),
                                        )
                                        Text(
                                            text = if (progress.downloaded == progress.total) "Done" else "Downloading",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.weight(0.2f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }

                        is ImageDownloadState.Done -> {

                        }

                        is ImageDownloadState.Error -> {

                        }
                    }

                }
            }
        },
        confirmButton = {
            if (state !is ImageDownloadState.Downloading) {
                Button(
                    onClick = onDismissRequest,
                ) {
                    Text(
                        text = stringResource(R.string.action_confirm)
                    )
                }
            }
        },
    )
}