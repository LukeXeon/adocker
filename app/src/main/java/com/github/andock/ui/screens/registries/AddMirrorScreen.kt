package com.github.andock.ui.screens.registries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import com.github.andock.R
import com.github.andock.ui.route.Route
import com.github.andock.ui.screens.main.LocalNavController
import com.github.andock.ui.theme.IconSize
import com.github.andock.ui.theme.Spacing
import com.github.andock.ui.utils.debounceClick
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMirrorScreen() {
    val navController = LocalNavController.current
    val onNavigateBack = debounceClick {
        navController.popBackStack()
    }
    val viewModel = hiltViewModel<RegistriesViewModel>()
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://") }
    var token by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("50") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var urlError by remember { mutableStateOf<String?>(null) }

    // Get error messages in Composable context
    val nameRequiredError = stringResource(R.string.mirror_settings_name_required)
    val urlHintError = stringResource(R.string.mirror_settings_url_hint)
    val urlInvalidError = stringResource(R.string.mirror_settings_url_invalid)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.mirror_settings_add_dialog_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(Spacing.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            item {
                Text(
                    text = stringResource(R.string.mirror_settings_add_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Spacer(modifier = Modifier.height(Spacing.Small))
            }

            // Name field
            item {
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
            }

            // URL field
            item {
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
            }

            // Priority field
            item {
                OutlinedTextField(
                    value = priority,
                    onValueChange = { priority = it },
                    label = { Text(stringResource(R.string.mirror_settings_priority_label)) },
                    placeholder = { Text("50") },
                    supportingText = { Text(stringResource(R.string.mirror_settings_priority_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Token field
            item {
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text(stringResource(R.string.mirror_settings_token_label)) },
                    placeholder = { Text(stringResource(R.string.mirror_settings_token_placeholder)) },
                    supportingText = { Text(stringResource(R.string.mirror_settings_token_hint)) },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Action buttons
            item {
                Spacer(modifier = Modifier.height(Spacing.Large))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onNavigateBack) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    FilledTonalButton(
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
                                val name = name.trim()
                                val url = url.trim().removeSuffix("/")
                                val token = token.trim().takeIf { it.isNotEmpty() }
                                viewModel.viewModelScope.launch {
                                    viewModel.addCustomMirror(name, url, token)
                                }
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.width(IconSize.Small)
                        )
                        Spacer(modifier = Modifier.width(Spacing.Small))
                        Text(stringResource(R.string.action_add))
                    }
                }
            }
        }
    }
}
