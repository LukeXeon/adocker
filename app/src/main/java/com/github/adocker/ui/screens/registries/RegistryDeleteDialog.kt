package com.github.adocker.ui.screens.registries

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
import com.github.adocker.R
import com.github.adocker.daemon.registries.Registry

@Composable
fun RegistryDeleteDialog(
    registry: Registry?,
    onDelete: (Registry) -> Unit,
    onDismissRequest: () -> Unit
) {
    val metadata = registry?.metadata?.collectAsState()?.value ?: return
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Default.Delete, contentDescription = null) },
        title = { Text(stringResource(R.string.mirror_settings_delete_title)) },
        text = { Text(stringResource(R.string.mirror_settings_delete_message, metadata.name)) },
        confirmButton = {
            TextButton(
                onClick = {
                    onDelete(registry)
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