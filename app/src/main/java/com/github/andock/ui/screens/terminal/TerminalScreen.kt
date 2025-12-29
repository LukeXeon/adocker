package com.github.andock.ui.screens.terminal

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun TerminalScreen(
    containerId: String
) {
    val viewModel = hiltViewModel<TerminalViewModel>()

}