package com.github.andock.ui.screens.containers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import com.github.andock.R
import com.github.andock.daemon.containers.Container

@Composable
fun ContainerDeleteDialog(
    container: Container?,
    onDelete: (Container) -> Unit,
    onDismissRequest: () -> Unit
) {
    val metadata = container?.metadata?.collectAsState()?.value ?: return
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Default.Delete, contentDescription = null) },
        title = { Text(stringResource(R.string.containers_delete_confirm_title)) },
        text = {
            Text(
                stringResource(
                    R.string.containers_delete_confirm_message,
                    metadata.name
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDelete(container)
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}