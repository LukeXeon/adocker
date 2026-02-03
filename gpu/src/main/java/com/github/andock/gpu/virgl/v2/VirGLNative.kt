package com.github.andock.gpu.virgl.v2

internal object VirGLNative {
    init {
        System.loadLibrary("virgl_jni")
    }


}