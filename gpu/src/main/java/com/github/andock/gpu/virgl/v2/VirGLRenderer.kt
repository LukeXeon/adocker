package com.github.andock.gpu.virgl.v2

import android.app.Application
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VirGLRenderer @Inject constructor(
    private val application: Application,
    private val eglManager: EGLManager,
    private val factory: VirGLContext.Factory,
) {

}