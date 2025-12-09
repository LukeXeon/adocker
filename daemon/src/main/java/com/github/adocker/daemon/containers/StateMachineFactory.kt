package com.github.adocker.daemon.containers

import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.github.adocker.daemon.app.AppContext
import com.github.adocker.daemon.database.dao.ContainerDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class StateMachineFactory @Inject constructor(
    containerDao: ContainerDao,
    appContext: AppContext,
    engine: PRootEngine,
) : FlowReduxStateMachineFactory<ContainerState, ContainerOperation>() {
    init {
        spec {
            inState<ContainerState.None> {
                on<ContainerOperation.Load> {
                    val entity = containerDao.getContainerById(it.containerId)
                    when {
                        entity == null -> {
                            override {
                                ContainerState.Dead(
                                    it.containerId,
                                    IllegalStateException("")
                                )
                            }
                        }

                        entity.lastRunAt == null -> {
                            override {
                                ContainerState.Exited(it.containerId)
                            }
                        }

                        else -> {
                            override {
                                ContainerState.Created(it.containerId)
                            }
                        }
                    }
                }
            }
            inState<ContainerState.Created> {

            }
            inState<ContainerState.Running> {

            }
            inState<ContainerState.Paused> {

            }
            inState<ContainerState.Exited> {

            }
            inState<ContainerState.Removing> {

            }
            inState<ContainerState.Restarting> {

            }
            inState<ContainerState.Dead> {

            }
        }
    }


}