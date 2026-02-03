package com.github.andock.gpu.virgl.v2

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

    fun start(address: LocalSocketAddress) {
        // Create server socket
        val localSocket = LocalSocket()
        localSocket.bind(address)
        val localServerSocket = LocalServerSocket(address)
    }
}