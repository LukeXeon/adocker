package com.github.adocker.daemon.registries

import com.github.adocker.daemon.database.dao.RegistryDao
import com.github.adocker.daemon.database.model.RegistryEntity
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

class Registry @AssistedInject constructor(
    @Assisted
    id: String,
    stateMachineFactory: RegistryStateMachine.Factory,
    parent: CoroutineScope,
    private val registryDao: RegistryDao,
) {
    private val scope = CoroutineScope(
        SupervisorJob(parent.coroutineContext[Job]) + Dispatchers.IO
    )

    private val stateMachine = stateMachineFactory.create(id).launchIn(scope)

    init {
        scope.launch {
            stateMachine.state.collect {
                if (it is RegistryState.Removed) {
                    scope.cancel()
                }
            }
        }
    }

    val id
        get() = state.value.id

    val state
        get() = stateMachine.state

    suspend fun getMetadata(): Result<RegistryEntity> {
        val entity = registryDao.getRegistryById(id)
        return if (entity != null) {
            Result.success(entity)
        } else {
            Result.failure(NoSuchElementException("Registry server not found: $id"))
        }
    }

    suspend fun check() {
        stateMachine.dispatch(RegistryOperation.Check)
    }

    suspend fun remove() {
        stateMachine.dispatch(RegistryOperation.Remove)
    }

    @Singleton
    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted
            id: String,
        ): Registry
    }
}