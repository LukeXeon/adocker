package com.github.andock.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.andock.daemon.containers.ContainerManager
import com.github.andock.daemon.containers.ContainerState
import com.github.andock.daemon.engine.PRootEngine
import com.github.andock.daemon.images.ImageManager
import com.github.andock.daemon.images.ImageReference
import com.github.andock.daemon.os.ProcessLimitCompat
import com.github.andock.common.withAtLeast
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    containerManager: ContainerManager,
    private val imageManager: ImageManager,
    private val prootEngine: PRootEngine,
    private val processLimitCompat: ProcessLimitCompat,
) : ViewModel() {

    val totalImages = imageManager.images.map { it.size }

    val totalContainers = containerManager.containers.map { it.size }

    val runningContainers = containerManager.filterState {
        it is ContainerState.Running
    }.map { it.size }

    val stoppedContainers = containerManager.filterState {
        it is ContainerState.Stopping
    }.map { it.size }

    private val _isShowWarning = MutableStateFlow<Boolean?>(null)

    val isShowWarning = _isShowWarning.asStateFlow()

    val prootVersion
        get() = prootEngine.version

    fun pullImage(imageName: ImageReference) = imageManager.pullImage(imageName)

    fun dismissWarning() {
        _isShowWarning.value = false
    }

    init {
        viewModelScope.launch {
            _isShowWarning.value = withAtLeast(1000) {
                !processLimitCompat.isUnrestricted()
            }
        }
    }

}