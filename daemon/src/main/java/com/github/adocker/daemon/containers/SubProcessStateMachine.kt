package com.github.adocker.daemon.containers

import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.completeWith
import kotlinx.coroutines.runInterruptible
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
class SubProcessStateMachine @AssistedInject constructor(
    @Assisted
    initialState: SubProcessState,
    private val processBuilder: ContainerProcessBuilder
) : FlowReduxStateMachineFactory<SubProcessState, Unit>() {

    init {
        initializeWith { initialState }
        spec {
            inState<SubProcessState.Creating> {
                onEnter {
                    val process = processBuilder.startProcess(
                        snapshot.containerId,
                        snapshot.command
                    )
                    snapshot.deferred.completeWith(process)
                    process.fold(
                        { process ->
                            override {
                                SubProcessState.Running(process)
                            }
                        },
                        { exception ->
                            override {
                                SubProcessState.Error(exception)
                            }
                        }
                    )
                }
            }
            inState<SubProcessState.Running> {
                onEnter {
                    runInterruptible {
                        snapshot.process.waitFor()
                    }
                    override {
                        SubProcessState.Exited(process)
                    }
                }
            }
        }
    }

    @Singleton
    @AssistedFactory
    interface Factory {
        fun create(@Assisted initialState: SubProcessState): SubProcessStateMachine
    }
}