package com.github.adocker.daemon.containers

import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow

class Container @AssistedInject constructor() {
    val state: Flow<ContainerState> = TODO()

}