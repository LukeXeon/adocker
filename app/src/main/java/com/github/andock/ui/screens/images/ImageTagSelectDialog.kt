package com.github.andock.ui.screens.images

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.github.andock.R
import com.github.andock.daemon.images.ImageReference
import timber.log.Timber

@Composable
fun ImageTagSelectDialog(
    registry: String,
    repository: String,
    onSelected: (ImageReference) -> Unit,
    onDismissRequest: () -> Unit
) {
    val viewModel = hiltViewModel<ImagesViewModel>()
    val (tags, setTags) = remember { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(Unit) {
        viewModel.getTags(registry, repository).fold(
            {
                setTags(it.toList())
            },
            {
                Timber.e(it)
                onDismissRequest()
            }
        )
    }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Default.Tag, contentDescription = null) },
        title = { Text(stringResource(R.string.images_tag)) },
        text = {
            LazyColumn(Modifier.fillMaxWidth()) {
                items(tags, { it }) {
                    Text(
                        it,
                        Modifier.clickable(
                            onClick = {
                                onSelected(ImageReference.parse("$registry/$repository:$it"))
                            }
                        )
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}