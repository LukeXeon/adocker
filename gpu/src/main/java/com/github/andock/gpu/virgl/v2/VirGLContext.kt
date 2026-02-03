package com.github.andock.gpu.virgl.v2

import android.graphics.SurfaceTexture
import android.net.LocalSocket
import android.view.Surface
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.io.Closeable
import javax.inject.Singleton

class VirGLContext @AssistedInject constructor(
    @Assisted("id")
    val id: Long,
    @Assisted("socket")
    private val localSocket: LocalSocket,
    private val eglManager: EGLManager,
) : Closeable {
    private val surfaceTexture = SurfaceTexture(false)
    private val surface = Surface(surfaceTexture)
    private val eglSurface = eglManager.createWindowSurface(surface)

    override fun close() {
        eglManager.destroySurface(eglSurface)
        localSocket.close()
        surface.release()
        surfaceTexture.release()
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