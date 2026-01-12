package com.github.andock.ui.screens.containers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.andock.daemon.containers.ContainerManager
import com.github.andock.daemon.containers.ContainerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ContainersViewModel @Inject constructor(
    private val containerManager: ContainerManager,
) : ViewModel() {

    val containers = containerManager.containers

    val sortedList = containerManager.sortedList

    val stateCounts = sortedList.flatMapLatest { containers ->
        combine(containers.map { container -> container.state }) { states ->
            ContainerFilterType.entries.asSequence().map {
                it to states.asSequence().filter(it.predicate).count()
            }.toMap()
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    fun filterState(predicate: (ContainerState) -> Boolean) =
        containerManager.filterState(predicate)

    // Delete a container
    suspend fun deleteContainer(containerId: String) {
        containers.value[containerId]?.remove()
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

}