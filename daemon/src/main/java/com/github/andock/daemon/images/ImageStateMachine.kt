package com.github.andock.daemon.images

import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
class ImageStateMachine @AssistedInject constructor(
    @Assisted
    initialState: ImageState
) : FlowReduxStateMachineFactory<ImageState, ImageOperation>() {

    init {
        initializeWith { initialState }
        spec {
            inState<ImageState.Downloading> {
                onEnter {

                    noChange()
                }

            }
            inState<ImageState.Downloaded> {
                onEnter {

                    noChange()
                }
            }
            inState<ImageState.Removing> {
                onEnter {

                    noChange()
                }
            }
        }
    }

    @Singleton
    @AssistedFactory
    interface Factory {
        fun create(@Assisted initialState: ImageState): ImageStateMachine
    }
}