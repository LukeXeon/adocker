package com.github.andock.ui.screens.containers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.github.andock.daemon.containers.ContainerManager
import com.github.andock.daemon.containers.shell.ContainerShell
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class ContainerExecViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    containerManager: ContainerManager
) : ViewModel() {
    val containerId = savedStateHandle.toRoute<ContainerExecRoute>().containerId

    val container = containerManager.containers.map { it[containerId] }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val _shell = MutableStateFlow<ContainerShell?>(null)
    val shell = _shell.asStateFlow()
    suspend fun startShell(): Boolean {
        container.filterNotNull().first().shell().fold(
            {
                _shell.value = it
                return true
            },
            {
                Timber.e(it)
                return false
            }
        )
    }

    init {
        viewModelScope.launch {
            try {
                startShell()
                awaitCancellation()
            } finally {
                _shell.value?.stop()
            }
        }
    }
}