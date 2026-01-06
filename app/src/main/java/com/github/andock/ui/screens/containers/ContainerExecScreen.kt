package com.github.andock.ui.screens.containers

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.github.andock.ui.route.Route

@Route(ContainerExecRoute::class)
@Composable
fun ContainerExecScreen() {
    val viewModel = hiltViewModel<ContainerExecViewModel>()
}