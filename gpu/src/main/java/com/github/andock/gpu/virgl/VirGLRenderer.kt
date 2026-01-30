package com.github.andock.gpu.virgl

import android.graphics.SurfaceTexture
import android.net.LocalSocket
import android.view.Surface
import java.io.Closeable

class VirGLRenderer(
    val id: Long,
    val localSocket: LocalSocket,
) : Closeable {
    val surfaceTexture = SurfaceTexture(false)
    private val surface = Surface(surfaceTexture)

    override fun close() {

    }

    init {
        surfaceTexture.setDefaultBufferSize(1280, 720)
    }


}
