package com.github.andock.daemon.images

import com.github.andock.daemon.database.dao.ImageDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Singleton

class Image @AssistedInject constructor(
    private val imageDao: ImageDao,
    factory: ImageStateMachine.Factory,
    parent: CoroutineScope,
    @Assisted initialState: ImageState
) {
    private val scope = CoroutineScope(
        SupervisorJob(parent.coroutineContext[Job]) + Dispatchers.IO
    )
    private val stateMachine = factory.create(initialState).launchIn(scope)

    val state
        get() = stateMachine.state

    val id
        get() = state.value.id

    val metadata
        get() = imageDao.getImageFlowById(id).stateIn(
            scope,
            SharingStarted.Eagerly,
            null
        )

    init {
        scope.launch {
            stateMachine.state.collect {
                if (it is ImageState.Removed) {
                    scope.cancel()
                }
            }
        }
    }

    @Singleton
    @AssistedFactory
    interface Factory {
        fun create(@Assisted initialState: ImageState): Image
    }
}