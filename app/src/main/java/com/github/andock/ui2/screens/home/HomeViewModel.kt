package com.github.andock.ui2.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.andock.daemon.containers.ContainerManager
import com.github.andock.daemon.containers.ContainerState
import com.github.andock.daemon.images.ImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    containerManager: ContainerManager,
    imagesRepository: ImageRepository
) : ViewModel() {

    val stats = containerManager.containers.combine(
        imagesRepository.getAllImages()
    ) { containers, images ->
        containers.values.toList() to images
    }.map { (containers, images) ->
        val runningCount = containers.count {
            it.state.value is ContainerState.Running
        }
        HomeStats(
            totalImages = images.size,
            totalContainers = containers.size,
            runningContainers = runningCount,
            stoppedContainers = containers.size - runningCount
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, HomeStats())
}