package com.github.andock.ui.screens.containers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.andock.daemon.client.model.ContainerConfig
import com.github.andock.daemon.containers.ContainerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContainersViewModel @Inject constructor(
    private val containerManager: ContainerManager,
) : ViewModel() {

    val containers = containerManager.containers

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

    // Create a container
    fun createContainer(
        imageId: String,
        name: String?,
        config: ContainerConfig = ContainerConfig()
    ) {
        viewModelScope.launch {
            containerManager.createContainer(imageId, name, config)
//                .onSuccess { container ->
//                    container.getInfo().onSuccess { entity ->
//                        _message.value = "Container created: ${entity.name}"
//                    }
//                }
//                .onFailure { e ->
//                    _error.value = "Create failed: ${e.message}"
//                }
        }
    }

    // Run container (create and start)
    fun runContainer(
        imageId: String,
        name: String?,
        config: ContainerConfig = ContainerConfig()
    ) {
        viewModelScope.launch {
            containerManager.createContainer(imageId, name, config).map {
                it.start()
            }
//                .onSuccess { container ->
//                    try {
//                        container.start()
//                        container.getInfo().onSuccess { entity ->
//                            _message.value = "Container running: ${entity.name}"
//                        }
//                    } catch (e: Exception) {
//                        _error.value = "Run failed: ${e.message}"
//                    }
//                }
//                .onFailure { e ->
//                    _error.value = "Create failed: ${e.message}"
//                }
        }
    }
}