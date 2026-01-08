package com.github.andock.ui.screens.containers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MoveToInbox
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.andock.R
import com.github.andock.daemon.containers.creator.ContainerCreateState
import com.github.andock.daemon.containers.creator.ContainerCreator
import com.github.andock.ui.theme.IconSize


@Composable
fun ContainerCreateDialog(
    creator: ContainerCreator,
    onDismissRequest: () -> Unit
) {
    val id = creator.id
    val state = creator.state.collectAsState().value
    val onDismissRequest = remember(creator, onDismissRequest) {
        {
            creator.cancel()
            onDismissRequest()
        }
    }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            when (state) {
                is ContainerCreateState.Done -> Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null
                )

                is ContainerCreateState.Error -> Icon(
                    Icons.Default.Error,
                    contentDescription = null
                )

                else -> Icon(
                    Icons.Default.MoveToInbox,
                    contentDescription = null
                )
            }
        },
        title = {
            when (state) {
                is ContainerCreateState.Done -> Text("容器创建成功")
                is ContainerCreateState.Error -> Text("容器创建成功")
                else -> Text("容器创建中")
            }
        },
        text = {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    when (state) {
                        is ContainerCreateState.Creating -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    trackColor = Color.LightGray.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = stringResource(R.string.status_loading),
                                    modifier = Modifier.padding(top = 16.dp),
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        is ContainerCreateState.Done -> {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
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
                                            imageVector = Icons.Default.ViewInAr,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(IconSize.Medium)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = id,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }

                        is ContainerCreateState.Error -> {
                            Text(
                                text = state.throwable.message ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                }
            }
        },
        confirmButton = {
            if (state !is ContainerCreateState.Creating) {
                Button(
                    onClick = onDismissRequest,
                ) {
                    Text(
                        text = stringResource(R.string.action_confirm)
                    )
                }
            }
        },
        dismissButton = {
            if (state is ContainerCreateState.Creating) {
                Button(
                    onClick = onDismissRequest,
                ) {
                    Text(
                        text = stringResource(R.string.action_cancel)
                    )
                }
            }
        }
    )
}