package com.github.andock.ui.screens.containers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.andock.daemon.containers.ContainerManager
import com.github.andock.daemon.containers.ContainerState
import com.github.andock.daemon.images.models.ContainerConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContainersViewModel @Inject constructor(
    private val containerManager: ContainerManager,
) : ViewModel() {

    val containers = containerManager.containers

    val sortedList = containerManager.sortedList

    fun stateList(predicate: (ContainerState) -> Boolean) = containerManager.stateList(predicate)

    // Delete a container
    suspend fun deleteContainer(containerId: String) {
        containers.value[containerId]?.remove()
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

    // Create a container
    suspend fun createContainer(
        imageId: String,
        name: String?,
        config: ContainerConfig = ContainerConfig()
    ) {
        containerManager.createContainer(imageId, name, config)
    }

    // Run container (create and start)
    suspend fun runContainer(
        imageId: String,
        name: String?,
        config: ContainerConfig = ContainerConfig()
    ) {
        containerManager.createContainer(imageId, name, config).map {
            it.start()
        }
    }
}