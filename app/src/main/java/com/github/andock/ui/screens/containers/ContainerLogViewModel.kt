package com.github.andock.ui.screens.containers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import androidx.paging.cachedIn
import com.github.andock.daemon.containers.ContainerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ContainerLogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    containerManager: ContainerManager,
) : ViewModel() {
    val containerId = savedStateHandle.toRoute<ContainerLogKey>().containerId

    val metadata = containerManager.containers.map {
        it[containerId]
    }.filterNotNull().flatMapLatest { it.metadata }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val logLines = containerManager.containers.map {
        it[containerId]
    }.filterNotNull().flatMapLatest {
        it.logLines
    }.cachedIn(viewModelScope)

}