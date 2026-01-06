package com.github.andock.ui.screens.containers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.github.andock.daemon.containers.ContainerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContainerDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val containerManager: ContainerManager,
) : ViewModel() {
    val containerId = savedStateHandle.toRoute<ContainerDetailRoute>().containerId

    val container = containerManager.containers.map { it[containerId] }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)


    // Stop a container
    fun stopContainer(containerId: String) {
        viewModelScope.launch {
            containerManager.containers.value[containerId]?.stop()
        }
    }

    // Start a container
    fun startContainer(containerId: String) {
        viewModelScope.launch {
            containerManager.containers.value[containerId]?.start()
        }
    }

    // Delete a container
    suspend fun deleteContainer(containerId: String) {
        containerManager.containers.value[containerId]?.remove()
    }
}