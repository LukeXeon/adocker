package com.github.andock.ui.screens.containers

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun ContainerExecScreen() {
    val viewModel = hiltViewModel<ContainerExecViewModel>()
}