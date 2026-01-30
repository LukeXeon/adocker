package com.github.andock.gpu.virgl

import android.graphics.SurfaceTexture
import android.view.Surface

class VirGLRenderer(
    val id: Long
) {
    val surfaceTexture = SurfaceTexture(false)
    private val surface = Surface(surfaceTexture)


}
