package com.github.andock.ui.screens.home

import androidx.lifecycle.ViewModel
import com.github.andock.daemon.containers.ContainerManager
import com.github.andock.daemon.containers.ContainerState
import com.github.andock.daemon.engine.PRootEngine
import com.github.andock.daemon.images.downloader.ImageDownloader
import com.github.andock.daemon.images.ImageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    containerManager: ContainerManager,
    private val imageManager: ImageManager,
    private val prootEngine: PRootEngine,
) : ViewModel() {

    val totalImages = imageManager.images.map { it.size }

    val totalContainers = containerManager.containers.map { it.size }

    val runningContainers =
        containerManager.stateList { it is ContainerState.Running }.map { it.size }

    val stoppedContainers =
        containerManager.stateList { it is ContainerState.Stopping }.map { it.size }

    val prootVersion
        get() = prootEngine.version

    fun pullImage(imageName: String): ImageDownloader {
        return imageManager.pullImage(imageName)
    }
}