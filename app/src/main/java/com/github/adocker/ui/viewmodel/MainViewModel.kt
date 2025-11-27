package com.github.adocker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adocker.daemon.registry.model.ContainerConfig
import com.github.adocker.daemon.database.model.ContainerEntity
import com.github.adocker.daemon.database.model.ImageEntity
import com.github.adocker.daemon.registry.model.SearchResult
import com.github.adocker.daemon.containers.ContainerRepository
import com.github.adocker.daemon.images.ImageRepository
import com.github.adocker.daemon.images.PullProgress
import com.github.adocker.daemon.containers.ContainerExecutor
import com.github.adocker.daemon.containers.RunningContainer
import com.github.adocker.ui.model.ContainerStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val imageRepository: ImageRepository,
    private val containerRepository: ContainerRepository,
    private val containerExecutor: ContainerExecutor,
    val json: Json
) : ViewModel() {

    // Images
    val images: StateFlow<List<ImageEntity>> = imageRepository.getAllImages()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Containers
    val containers: StateFlow<List<ContainerEntity>> = containerRepository.getAllContainers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Running containers
    val runningContainers: StateFlow<List<RunningContainer>> =
        containerExecutor.getAllRunningContainers().stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList()
        )

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
            containerRepository.createContainer(imageId, name, config)
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
            containerExecutor.startContainer(containerId)
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
            containerExecutor.stopContainer(containerId)
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
            containerRepository.deleteContainer(containerId)
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
            containerRepository.createContainer(imageId, name, config)
                .onSuccess { container ->
                    containerExecutor.startContainer(container.id)
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

    // Helper function to get container status
    fun getContainerStatus(container: ContainerEntity): ContainerStatus {
        val running = runningContainers.value.find { it.containerId == container.id }
        return when {
            running?.job?.isActive == true -> ContainerStatus.RUNNING
            container.hasRun -> ContainerStatus.EXITED
            else -> ContainerStatus.CREATED
        }
    }

    // Get running containers count
    fun getRunningCount(): Int {
        return runningContainers.value.count { it.job.isActive }
    }

    // Get stats
    data class Stats(
        val totalImages: Int,
        val totalContainers: Int,
        val runningContainers: Int,
        val stoppedContainers: Int
    )

    val stats: StateFlow<Stats> =
        combine(images, containers, runningContainers) { images, containers, running ->
            val runningCount = running.count { it.job.isActive }
            Stats(
                totalImages = images.size,
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
