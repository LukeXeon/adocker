package com.github.andock.ui.screens.containers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.github.andock.daemon.containers.ContainerManager
import com.github.andock.daemon.images.ImageManager
import com.github.andock.daemon.images.models.ContainerConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ContainerCreateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    imageManager: ImageManager,
    private val containerManager: ContainerManager
) : ViewModel() {
    val imageId = savedStateHandle.toRoute<ContainerCreateRoute>().imageId

    val image = imageManager.getImageById(imageId)
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            null
        )

    // Create a container
    fun createContainer(
        imageId: String,
        name: String?,
        config: ContainerConfig
    ) = containerManager.createContainer(imageId, name, config)

    // Run container (create and start)
    suspend fun runContainer(
        imageId: String,
        name: String?,
        config: ContainerConfig = ContainerConfig()
    ) {
        TODO()
    }
}