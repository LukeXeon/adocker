package com.github.andock.ui.screens.containers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.andock.daemon.containers.ContainerManager
import com.github.andock.daemon.images.ImageManager
import com.github.andock.daemon.images.models.ContainerConfig
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel(assistedFactory = ContainerCreateViewModel.Factory::class)
class ContainerCreateViewModel @AssistedInject constructor(
    @Assisted private val navKey: ContainerCreateKey,
    imageManager: ImageManager,
    private val containerManager: ContainerManager
) : ViewModel() {
    val imageId = navKey.imageId

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

    @AssistedFactory
    interface Factory {
        fun create(navKey: ContainerCreateKey): ContainerCreateViewModel
    }
}