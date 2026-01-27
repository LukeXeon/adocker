package com.github.andock.ui.screens.containers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.andock.daemon.containers.ContainerManager
import com.github.andock.daemon.containers.shell.ContainerShell
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber


@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = ContainerExecViewModel.Factory::class)
class ContainerExecViewModel @AssistedInject constructor(
    @Assisted private val navKey: ContainerExecKey,
    containerManager: ContainerManager
) : ViewModel() {
    val containerId = navKey.containerId
    private val container = containerManager.containers.map { it[containerId] }
    val metadata = container
        .filterNotNull()
        .flatMapLatest {
            it.metadata
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
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
                withContext(NonCancellable) {
                    _shell.value?.stop()
                }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(navKey: ContainerExecKey): ContainerExecViewModel
    }
}