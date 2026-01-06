package com.github.andock.ui.screens.containers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import com.github.andock.daemon.containers.ContainerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ContainerExecViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val containerManager: ContainerManager
) : ViewModel() {
    val containerId = savedStateHandle.toRoute<ContainerExecRoute>().containerId


}