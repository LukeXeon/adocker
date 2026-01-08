package com.github.andock.daemon.containers.shell

import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import com.github.andock.daemon.database.dao.InMemoryLogDao
import com.github.andock.daemon.database.model.InMemoryLogEntity
import com.github.andock.daemon.os.await
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import timber.log.Timber
import java.util.UUID
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
class ContainerShellStateMachine @AssistedInject constructor(
    @Assisted
    process: Process,
    inMemoryLogStore: InMemoryLogDao,
) : FlowReduxStateMachineFactory<ContainerShellState, Unit>() {

    init {
        initializeWith {
            ContainerShellState.Running(UUID.randomUUID().toString(), process)
        }
        spec {
            inState<ContainerShellState.Running> {
                onEnter {
                    val (sessionId, process) = snapshot
                    val exitCode = try {
                        process.inputStream.bufferedReader().useLines { lines ->
                            lines.forEach { line ->
                                inMemoryLogStore.append(
                                    InMemoryLogEntity(
                                        id = 0,
                                        sessionId = sessionId,
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
                            Timber.e(e)
                            process.await()
                        }
                    }
                    inMemoryLogStore.deleteById(sessionId = sessionId)
                    override {
                        ContainerShellState.Exited(id, exitCode)
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