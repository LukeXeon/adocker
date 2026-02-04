package com.github.andock.gpu.virgl

internal object VirGLNative {
    init {
        System.loadLibrary("virgl_jni")
    }

    @JvmStatic
    external fun initRenderer(display: Long, flags: Int): Int
}