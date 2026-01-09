package com.github.andock.daemon.containers.creator

import com.github.andock.daemon.images.models.ContainerConfig
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

class ContainerCreator @AssistedInject constructor(
    @Assisted("imageId")
    imageId: String,
    @Assisted("name")
    name: String?,
    @Assisted
    config: ContainerConfig,
    parent: CoroutineScope,
    factory: ContainerCreateStateMachine.Factory
) {
    private val scope = CoroutineScope(
        SupervisorJob(parent.coroutineContext[Job]) + Dispatchers.IO
    )

    private val stateMachine = factory.create(
        ContainerCreateState.Creating(
            UUID.randomUUID().toString(),
            imageId,
            name,
            config
        )
    ).launchIn(scope)

    val state
        get() = stateMachine.state

    val id
        get() = state.value.id

    fun cancel() {
        stateMachine.dispatchAction(CancellationException())
    }

    init {
        scope.launch {
            stateMachine.state.collect {
                if (it is ContainerCreateState.Done
                    || it is ContainerCreateState.Error
                ) {
                    scope.cancel()
                }
            }
        }
    }


    @Singleton
    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("imageId")
            imageId: String,
            @Assisted("name")
            name: String?,
            @Assisted
            config: ContainerConfig
        ): ContainerCreator
    }
}