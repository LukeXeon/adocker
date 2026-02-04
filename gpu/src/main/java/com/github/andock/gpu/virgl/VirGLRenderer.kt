package com.github.andock.gpu.virgl

import android.net.LocalServerSocket
import android.net.LocalSocket
import android.net.LocalSocketAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VirGLRenderer @Inject constructor(
    private val eglManager: EGLManager,
    private val factory: VirGLContext.Factory,
) {

    companion object {
        private const val VIRGL_RENDERER_USE_EGL = 1
        private const val VIRGL_RENDERER_USE_EXTERNAL_BLOB = (1 shl 5)
    }

    init {
        if (eglManager.initialize()) {
            val flags = VIRGL_RENDERER_USE_EGL or VIRGL_RENDERER_USE_EXTERNAL_BLOB
            VirGLNative.initRenderer(eglManager.getDisplay()?.nativeHandle ?: 0, flags)
        }
    }

    private fun createGLContext(): Long {
        val context = eglManager.createContext() ?: return 0
    }

    fun start(address: LocalSocketAddress) {
        // Create server socket
        val localSocket = LocalSocket()
        localSocket.bind(address)
        val localServerSocket = LocalServerSocket(address)
    }
}