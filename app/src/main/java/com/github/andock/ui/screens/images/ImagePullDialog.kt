package com.github.andock.ui.screens.images

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.github.andock.R
import com.github.andock.ui.theme.Spacing

@Composable
fun ImagePullDialog(
    onPullImage: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var imageName by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Default.Download, contentDescription = null) },
        title = { Text(stringResource(R.string.pull_image_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.ListItemSpacing)
            ) {
                OutlinedTextField(
                    value = imageName,
                    onValueChange = { imageName = it },
                    label = { Text(stringResource(R.string.pull_image_name_label)) },
                    placeholder = { Text(stringResource(R.string.pull_image_name_placeholder)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (imageName.isNotBlank()) {
                                onPullImage(imageName)
                            }
                        }
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    focusManager.clearFocus()
                    if (imageName.isNotBlank()) {
                        onPullImage(imageName)
                    }
                },
                enabled = imageName.isNotBlank()
            ) {
                Text(
                    text = stringResource(R.string.action_pull)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
