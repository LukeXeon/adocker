package com.github.adocker.ui.screens.containers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adocker.daemon.containers.ContainerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContainersViewModel @Inject constructor(
    containerManager: ContainerManager,
) : ViewModel() {

    val containers = containerManager.allContainers

    // Delete a container
    fun deleteContainer(containerId: String) {
        viewModelScope.launch {
            containers.value[containerId]?.remove()
        }
    }

    // Stop a container
    fun stopContainer(containerId: String) {
        viewModelScope.launch {
            containers.value[containerId]?.stop()
        }
    }

    // Start a container
    fun startContainer(containerId: String) {
        viewModelScope.launch {
            containers.value[containerId]?.start()
        }
    }
}