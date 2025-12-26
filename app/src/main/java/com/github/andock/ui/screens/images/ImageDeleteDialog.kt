package com.github.andock.ui.screens.images

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

@Composable
fun ImageDeleteDialog(
    image: Image?,
    onDelete: (Image) -> Unit,
    onDismissRequest: () -> Unit
) {
    val metadata = image?.metadata?.collectAsState()?.value
    if (metadata != null) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text(stringResource(R.string.images_delete_confirm_title)) },
            text = {
                Text(stringResource(R.string.images_delete_confirm_message, metadata.fullName))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(image)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}