package com.github.andock.gpu.virgl

import android.net.LocalSocket
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.io.Closeable
import javax.inject.Singleton

class VirGLContext @AssistedInject constructor(
    @Assisted("id")
    val id: Long,
    @Assisted("socket")
    private val localSocket: LocalSocket
) : Closeable {

    override fun close() {
        localSocket.close()
    }

    @Singleton
    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("id")
            id: Long,
            @Assisted("socket")
            localSocket: LocalSocket
        ): VirGLContext
    }
}