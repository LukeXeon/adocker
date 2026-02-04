package com.github.andock.gpu.virgl

import android.graphics.SurfaceTexture
import android.view.Surface
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.io.Closeable
import javax.inject.Singleton

class VirGLRendererContext @AssistedInject constructor(
    private val eglManager: EGLManager,
) : Closeable {
    private val context = eglManager.createContext()
    private val surfaceTexture = SurfaceTexture(false)
    private val surface = Surface(surfaceTexture)
    private val eglSurface = eglManager.createWindowSurface(surface)

    init {
        surfaceTexture.setDefaultBufferSize(1280, 720)
    }

    override fun close() {
        eglManager.destroySurface(eglSurface)
        surface.release()
        surfaceTexture.release()
    }

    fun makeCurrent() {
        eglManager.makeCurrent(context, eglSurface)
    }

    @Singleton
    @AssistedFactory
    interface Factory {
        fun create(): VirGLRendererContext
    }
}