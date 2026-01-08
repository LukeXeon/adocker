package com.github.andock.ui.screens.containers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.github.andock.daemon.containers.Container.Companion.N
import com.github.andock.daemon.containers.ContainerManager
import com.github.andock.daemon.database.dao.InMemoryLogDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject


@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ContainerExecViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    containerManager: ContainerManager,
    private val inMemoryLogStore: InMemoryLogDao
) : ViewModel() {

    val containerId = savedStateHandle.toRoute<ContainerExecRoute>().containerId
    private val sessionId = UUID.randomUUID().toString()
    private val pager = Pager(
        config = PagingConfig(
            pageSize = N,
            enablePlaceholders = false,
            initialLoadSize = N,
        ),
        initialKey = 1,
        pagingSourceFactory = {
            inMemoryLogStore.getLogLinesPaged(sessionId = sessionId)
        }
    )

    val logLines = pager.flow.cachedIn(viewModelScope)

    val container = containerManager.containers.map { it[containerId] }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            try {
                awaitCancellation()
            } finally {
                withContext(NonCancellable) {
                    inMemoryLogStore.clearLogById(sessionId)
                }
            }
        }
    }

    fun stopShell() {

    }

    fun executeCommand(it: String) {

    }

}