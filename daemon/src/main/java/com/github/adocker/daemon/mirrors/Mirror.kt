package com.github.adocker.daemon.mirrors

import com.github.adocker.daemon.database.dao.MirrorDao
import com.github.adocker.daemon.database.model.MirrorEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Singleton

class Mirror @AssistedInject constructor(
    @Assisted
    id: String,
    stateMachineFactory: MirrorStateMachine.Factory,
    parent: CoroutineScope,
    private val mirrorDao: MirrorDao,
) {
    private val scope = CoroutineScope(
        SupervisorJob(parent.coroutineContext[Job]) + Dispatchers.IO
    )

    private val stateMachine = stateMachineFactory.create(id).launchIn(scope)

    init {
        scope.launch {
            stateMachine.state.collect {
                if (it is MirrorState.Deleted) {
                    scope.cancel()
                }
            }
        }
    }

    val id
        get() = state.value.id

    val state
        get() = stateMachine.state

    suspend fun getMetadata(): Result<MirrorEntity> {
        val entity = mirrorDao.getMirrorById(id)
        return if (entity != null) {
            Result.success(entity)
        } else {
            Result.failure(NoSuchElementException("Mirror not found: $id"))
        }
    }

    suspend fun check() {
        stateMachine.dispatch(MirrorOperation.Check)
    }

    suspend fun delete() {
        stateMachine.dispatch(MirrorOperation.Delete)
    }


    @Singleton
    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted
            id: String,
        ): Mirror
    }
}