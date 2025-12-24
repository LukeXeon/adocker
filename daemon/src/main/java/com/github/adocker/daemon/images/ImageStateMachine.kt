package com.github.adocker.daemon.images

import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Singleton

class ImageStateMachine @AssistedInject constructor() :
    FlowReduxStateMachineFactory<ImageState, ImageOperation>() {


    @Singleton
    @AssistedFactory
    interface Factory {
        fun create(): ImageStateMachine
    }

}