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
class ProcessStateMachineFactory @AssistedInject constructor(
    @Assisted
    initialState: ProcessState,
    private val processBuilder: ContainerProcessBuilder
) : FlowReduxStateMachineFactory<ProcessState, Unit>() {

    init {
        initializeWith { initialState }
        spec {
            inState<ProcessState.Starting> {
                onEnter {
                    val process = processBuilder.startProcess(
                        snapshot.containerId,
                        snapshot.command
                    )
                    snapshot.deferred.completeWith(process)
                    process.fold(
                        { process ->
                            override {
                                ProcessState.Running(process)
                            }
                        },
                        { exception ->
                            override {
                                ProcessState.Abort(exception)
                            }
                        }
                    )
                }
            }
            inState<ProcessState.Running> {
                onEnter {
                    runInterruptible {
                        snapshot.process.waitFor()
                    }
                    override {
                        ProcessState.Exited(process)
                    }
                }
            }
        }
    }

    @Singleton
    @AssistedFactory
    interface Builder {
        fun build(@Assisted initialState: ProcessState): ProcessStateMachineFactory
    }
}