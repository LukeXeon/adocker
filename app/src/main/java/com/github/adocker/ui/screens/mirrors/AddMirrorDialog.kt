package com.github.adocker.ui.screens.mirrors

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.github.adocker.R
import com.github.adocker.ui.theme.Spacing

@Composable
fun AddMirrorDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, url: String, token: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://") }
    var token by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("50") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var urlError by remember { mutableStateOf<String?>(null) }

    // Get error messages in Composable context
    val nameRequiredError = "Name is required"
    val urlHintError = stringResource(R.string.mirror_settings_url_hint)
    val urlInvalidError = "Invalid URL"

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        title = { Text(stringResource(R.string.mirror_settings_add_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null
                    },
                    label = { Text(stringResource(R.string.mirror_settings_name_label)) },
                    placeholder = { Text(stringResource(R.string.mirror_settings_name_placeholder)) },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(Spacing.Small))
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        urlError = null
                    },
                    label = { Text(stringResource(R.string.mirror_settings_url_label)) },
                    placeholder = { Text(stringResource(R.string.mirror_settings_url_placeholder)) },
                    isError = urlError != null,
                    supportingText = urlError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(Spacing.Small))
                OutlinedTextField(
                    value = priority,
                    onValueChange = { priority = it },
                    label = { Text("Priority (0-100)") },
                    placeholder = { Text("50") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(Spacing.Small))
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text(stringResource(R.string.mirror_settings_token_label)) },
                    placeholder = { Text(stringResource(R.string.mirror_settings_token_placeholder)) },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    var hasError = false
                    if (name.isBlank()) {
                        nameError = nameRequiredError
                        hasError = true
                    }
                    if (!url.startsWith("https://") && !url.startsWith("http://")) {
                        urlError = urlHintError
                        hasError = true
                    } else if (url.length < 10) {
                        urlError = urlInvalidError
                        hasError = true
                    }
                    if (!hasError) {
                        priority.toIntOrNull() ?: 50
                        onAdd(
                            name.trim(),
                            url.trim().removeSuffix("/"),
                            token.trim().takeIf { it.isNotEmpty() }
                        )
                    }
                }
            ) {
                Text(stringResource(R.string.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}