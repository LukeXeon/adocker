package com.github.andock.ui.screens.containers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Start
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import com.github.andock.R
import com.github.andock.daemon.containers.ContainerState
import com.github.andock.ui.route.Route
import com.github.andock.ui.screens.main.LocalNavController
import com.github.andock.ui.theme.Spacing
import com.github.andock.ui.utils.debounceClick
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Route(ContainerExecRoute::class)
@Composable
fun ContainerExecScreen() {
    val viewModel = hiltViewModel<ContainerExecViewModel>()
    val navController = LocalNavController.current
    val container = viewModel.container.collectAsState().value ?: return
    val metadata = container.metadata.collectAsState().value ?: return
    val state = container.state.collectAsState().value
    var inputText by remember { mutableStateOf("") }
    val isRunning = state is ContainerState.Running
    val onNavigateBack = debounceClick {
        navController.popBackStack()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = metadata.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = metadata.id,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.stopShell()
                        }
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = stringResource(R.string.terminal_clear)
                        )
                    }
                    if (isRunning) {
                        IconButton(
                            onClick = {
                                viewModel.viewModelScope.launch {
                                    container.stop()
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = stringResource(R.string.action_stop)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                viewModel.viewModelScope.launch {
                                    container.start()
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Start,
                                contentDescription = stringResource(R.string.action_start)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E1E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF1E1E1E),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Terminal output
//            LazyColumn(
//                state = listState,
//                modifier = Modifier
//                    .weight(1f)
//                    .fillMaxWidth()
//                    .background(Color(0xFF1E1E1E))
//                    .padding(horizontal = 8.dp),
//                verticalArrangement = Arrangement.spacedBy(2.dp)
//            ) {
//                items(outputLines) { line ->
//                    Text(
//                        text = line,
//                        fontFamily = FontFamily.Monospace,
//                        fontSize = 12.sp,
//                        color = getLineColor(line),
//                        modifier = Modifier.fillMaxWidth()
//                    )
//                }
//            }

            // Input field
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF2D2D2D)
            ) {
                Row(
                    modifier = Modifier
                        .padding(Spacing.Small)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$",
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                stringResource(R.string.terminal_input_placeholder),
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank()) {
                                    viewModel.executeCommand(inputText)
                                    inputText = ""
                                }
                            }
                        )
                    )

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.executeCommand(inputText)
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.terminal_send),
                            tint = if (inputText.isNotBlank()) Color(0xFF4CAF50) else Color.Gray
                        )
                    }
                }
            }
            // Quick commands
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF252525)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                ) {
                    QuickCommandChip("ls -la") {
                        viewModel.executeCommand(it)
                    }
                    QuickCommandChip("pwd") {
                        viewModel.executeCommand(it)
                    }
                    QuickCommandChip("whoami") {
                        viewModel.executeCommand(it)
                    }
                    QuickCommandChip("cat /etc/os-release") {
                        viewModel.executeCommand(it)
                    }
                }
            }
        }
    }
}