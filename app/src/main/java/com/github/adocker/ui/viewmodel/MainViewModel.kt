package com.github.adocker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adocker.daemon.containers.ContainerManager
import com.github.adocker.daemon.database.model.ImageEntity
import com.github.adocker.daemon.images.ImageRepository
import com.github.adocker.daemon.images.PullProgress
import com.github.adocker.daemon.registry.model.ContainerConfig
import com.github.adocker.daemon.registry.model.SearchResult
import com.github.adocker.ui.model.ContainerStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.http4k.server.Http4kServer
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val imageRepository: ImageRepository,
    private val containerManager: ContainerManager,
    val json: Json,
    servers: Set<@JvmSuppressWildcards Http4kServer>,
) : ViewModel() {

    init {
        servers.forEach { it.start() }
    }

    // Images
    val images: StateFlow<List<ImageEntity>> = imageRepository.getAllImages()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Containers
    val containers: StateFlow<List<Container2>> = containerManager.getAllContainers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Search results
    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    // Pull progress
    private val _pullProgress = MutableStateFlow<Map<String, PullProgress>>(emptyMap())
    val pullProgress: StateFlow<Map<String, PullProgress>> = _pullProgress.asStateFlow()

    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isPulling = MutableStateFlow(false)
    val isPulling: StateFlow<Boolean> = _isPulling.asStateFlow()

    // Error handling
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Success messages
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // Search images on Docker Hub
    fun searchImages(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            imageRepository.searchImages(query)
                .onSuccess { results ->
                    _searchResults.value = results
                }
                .onFailure { e ->
                    _error.value = "Search failed: ${e.message}"
                }
            _isLoading.value = false
        }
    }

    // Pull an image
    fun pullImage(imageName: String) {
        viewModelScope.launch {
            _isPulling.value = true
            _error.value = null

            try {
                imageRepository.pullImage(imageName)
                    .catch { e ->
                        _error.value = "Pull failed: ${e.message}"
                        _isPulling.value = false
                    }
                    .collect { progress ->
                        _pullProgress.value = _pullProgress.value.toMutableMap().apply {
                            put(progress.layerDigest, progress)
                        }
                    }

                _message.value = "Image pulled successfully: $imageName"
            } catch (e: Exception) {
                _error.value = "Pull failed: ${e.message}"
            } finally {
                _isPulling.value = false
                _pullProgress.value = emptyMap()
            }
        }
    }

    // Delete an image
    fun deleteImage(imageId: String) {
        viewModelScope.launch {
            imageRepository.deleteImage(imageId)
                .onSuccess {
                    _message.value = "Image deleted successfully"
                }
                .onFailure { e ->
                    _error.value = "Delete failed: ${e.message}"
                }
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
                .onSuccess { container ->
                    _message.value = "Container created: ${container.name}"
                }
                .onFailure { e ->
                    _error.value = "Create failed: ${e.message}"
                }
        }
    }

    // Start a container
    fun startContainer(containerId: String) {
        viewModelScope.launch {
            containerManager.startContainer(containerId)
                .onSuccess {
                    _message.value = "Container started"
                }
                .onFailure { e ->
                    _error.value = "Start failed: ${e.message}"
                }
        }
    }

    // Stop a container
    fun stopContainer(containerId: String) {
        viewModelScope.launch {
            containerManager.stopContainer(containerId)
                .onSuccess {
                    _message.value = "Container stopped"
                }
                .onFailure { e ->
                    _error.value = "Stop failed: ${e.message}"
                }
        }
    }

    // Delete a container
    fun deleteContainer(containerId: String) {
        viewModelScope.launch {
            containerManager.deleteContainer(containerId)
                .onSuccess {
                    _message.value = "Container deleted"
                }
                .onFailure { e ->
                    _error.value = "Delete failed: ${e.message}"
                }
        }
    }

    // Run container (create and start)
    fun runContainer(
        imageId: String,
        name: String?,
        config: ContainerConfig = ContainerConfig()
    ) {
        viewModelScope.launch {
            containerManager.createContainer(imageId, name, config)
                .onSuccess { container ->
                    containerManager.startContainer(container.id)
                        .onSuccess {
                            _message.value = "Container running: ${container.name}"
                        }
                        .onFailure { e ->
                            _error.value = "Run failed: ${e.message}"
                        }
                }
                .onFailure { e ->
                    _error.value = "Create failed: ${e.message}"
                }
        }
    }

    // Clear error
    fun clearError() {
        _error.value = null
    }

    // Clear message
    fun clearMessage() {
        _message.value = null
    }

    // Helper function to convert Container state to UI ContainerStatus
    fun getContainerStatus(container: Container2): ContainerStatus {
        return when (container.state) {
            com.github.adocker.daemon.containers.ContainerState2.Created -> ContainerStatus.CREATED
            com.github.adocker.daemon.containers.ContainerState2.Running -> ContainerStatus.RUNNING
            com.github.adocker.daemon.containers.ContainerState2.Exited -> ContainerStatus.EXITED
            else -> ContainerStatus.EXITED // For Paused, Restarting, Removing, Dead
        }
    }

    // Get running containers count
    fun getRunningCount(): Int {
        return containers.value.count {
            it.state == com.github.adocker.daemon.containers.ContainerState2.Running
        }
    }

    // Get stats
    data class Stats(
        val totalImages: Int,
        val totalContainers: Int,
        val runningContainers: Int,
        val stoppedContainers: Int
    )

    val stats: StateFlow<Stats> = containers.map { containers ->
        val runningCount = containers.count {
            it.state == com.github.adocker.daemon.containers.ContainerState2.Running
        }
        Stats(
            totalImages = images.value.size,
            totalContainers = containers.size,
            runningContainers = runningCount,
            stoppedContainers = containers.size - runningCount
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        Stats(0, 0, 0, 0)
    )
}
