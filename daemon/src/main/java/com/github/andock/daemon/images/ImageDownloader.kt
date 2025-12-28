package com.github.andock.daemon.images

import com.github.andock.daemon.client.ImageReference
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Singleton

class ImageDownloader @AssistedInject constructor(
    @Assisted
    imageRef: ImageReference,
    parent: CoroutineScope,
    factory: ImageDownloadStateMachine.Factory
) {
    private val scope = CoroutineScope(
        SupervisorJob(parent.coroutineContext[Job]) + Dispatchers.IO
    )

    private val stateMachine = factory.create(imageRef).launchIn(scope)

    init {
        scope.launch {
            stateMachine.state.collect {
                if (it is ImageDownloadState.Done
                    || it is ImageDownloadState.Error
                ) {
                    scope.cancel()
                }
            }
        }
    }

    val state
        get() = stateMachine.state

    val ref
        get() = state.value.ref

    fun cancel() {
        stateMachine.dispatchAction(CancellationException())
    }

    @Singleton
    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted
            imageRef: ImageReference,
        ): ImageDownloader
    }
}