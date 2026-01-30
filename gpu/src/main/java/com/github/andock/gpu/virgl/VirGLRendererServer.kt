package com.github.andock.gpu.virgl

import android.app.Application
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.net.LocalSocketAddress
import androidx.collection.MutableLongObjectMap
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VirGLRendererServer @Inject constructor(
    application: Application
) {

    init {
        System.loadLibrary("virgl_jni")
    }

    private val clients = MutableLongObjectMap<VirGLRenderer>()

    val path = File(
        application.cacheDir,
        "virglrenderer_pipe"
    )

    private val localSocket = LocalSocket()
    private val localServerSocket = LocalServerSocket(
        localSocket.apply {
            localSocket.bind(
                LocalSocketAddress(
                    path.absolutePath,
                    LocalSocketAddress.Namespace.FILESYSTEM
                )
            )
        }.fileDescriptor
    )

    fun start() {

    }
}