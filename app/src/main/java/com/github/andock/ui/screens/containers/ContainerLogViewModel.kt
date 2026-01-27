package com.github.andock.ui.screens.containers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.github.andock.daemon.containers.ContainerManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = ContainerLogViewModel.Factory::class)
class ContainerLogViewModel @AssistedInject constructor(
    @Assisted private val navKey: ContainerLogKey,
    containerManager: ContainerManager,
) : ViewModel() {
    val containerId = navKey.containerId

    val metadata = containerManager.containers.map {
        it[containerId]
    }.filterNotNull().flatMapLatest { it.metadata }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val logLines = containerManager.containers.map {
        it[containerId]
    }.filterNotNull().flatMapLatest {
        it.logLines
    }.cachedIn(viewModelScope)

    @AssistedFactory
    interface Factory {
        fun create(navKey: ContainerLogKey): ContainerLogViewModel
    }
}