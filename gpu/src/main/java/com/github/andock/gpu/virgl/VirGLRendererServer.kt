package com.github.andock.gpu.virgl

import androidx.collection.MutableLongObjectMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VirGLRendererServer @Inject constructor() {

    init {
        System.loadLibrary("virgl_jni")
    }

    private val clients = MutableLongObjectMap<VirGLRenderer>()

    fun start() {

    }
}