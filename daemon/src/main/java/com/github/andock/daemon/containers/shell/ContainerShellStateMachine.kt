package com.github.andock.daemon.containers.shell

import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import com.github.andock.daemon.database.dao.InMemoryLogDao
import com.github.andock.daemon.database.model.InMemoryLogEntity
import com.github.andock.daemon.os.await
import com.github.andock.daemon.os.id
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import timber.log.Timber
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
class ContainerShellStateMachine @AssistedInject constructor(
    @Assisted
    process: Process,
    inMemoryLogStore: InMemoryLogDao,
) : FlowReduxStateMachineFactory<ContainerShellState, Unit>() {

    init {
        initializeWith {
            ContainerShellState.Running(process)
        }
        spec {
            inState<ContainerShellState.Running> {
                onEnter {
                    val process = snapshot.process
                    try {
                        process.inputStream.bufferedReader().useLines { lines ->
                            lines.forEach { line ->
                                inMemoryLogStore.append(
                                    InMemoryLogEntity(
                                        id = 0,
                                        sessionId = process.id,
                                        timestamp = System.currentTimeMillis(),
                                        message = line
                                    )
                                )
                            }
                        }
                        process.await()
                    } catch (e: Exception) {
                        if (e is CancellationException) {
                            throw e
                        } else {
                            Timber.d(e)
                            process.await()
                        }
                    }
                    inMemoryLogStore.deleteById(sessionId = process.id)
                    override {
                        ContainerShellState.Exited(process)
                    }
                }
            }
        }
    }

    @Singleton
    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted
            process: Process
        ): ContainerShellStateMachine
    }
}