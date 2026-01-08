package com.github.andock.ui.screens.containers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.github.andock.daemon.containers.ContainerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ContainerExecViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val containerManager: ContainerManager
) : ViewModel() {
    val containerId = savedStateHandle.toRoute<ContainerExecRoute>().containerId

    val container = containerManager.containers.map { it[containerId] }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun stopShell() {

    }

    fun executeCommand(it: String) {

    }

}