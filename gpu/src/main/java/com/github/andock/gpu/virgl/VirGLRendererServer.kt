package com.github.andock.gpu.virgl

import android.app.Application
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.util.Log
import androidx.collection.MutableLongObjectMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VirGL Renderer Server - Singleton service
 *
 * Responsibilities:
 * - Global virglrenderer initialization (once)
 * - Unix socket server for client connections
 * - Creating and managing VirGLRenderer instances
 * - Lifecycle management
 */
@Singleton
class VirGLRendererServer @Inject constructor(
    private val application: Application
) {
    companion object {
        private const val TAG = "VirGLRendererServer"
        private const val VIRGL_RENDERER_USE_EGL = 1
        private const val VIRGL_RENDERER_USE_EXTERNAL_BLOB = (1 shl 5)
    }

    private val clients = MutableLongObjectMap<VirGLRenderer>()
    private var nextRendererId = 1L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    val path = File(
        application.cacheDir,
        "virglrenderer_pipe"
    )

    private val localSocket = LocalSocket()
    private val localServerSocket: LocalServerSocket

    init {
        // Load JNI library
        System.loadLibrary("virgl_jni")

        // Initialize virglrenderer globally (once)
        val flags = VIRGL_RENDERER_USE_EGL or VIRGL_RENDERER_USE_EXTERNAL_BLOB
        val result = VirGLNative.initRenderer(flags)
        if (result < 0) {
            Log.e(TAG, "Failed to initialize virglrenderer: $result")
            throw RuntimeException("VirGL initialization failed: $result")
        }
        Log.i(TAG, "VirGL renderer initialized successfully")

        // Create server socket
        localSocket.bind(
            LocalSocketAddress(
                path.absolutePath,
                LocalSocketAddress.Namespace.FILESYSTEM
            )
        )
        localServerSocket = LocalServerSocket(localSocket.fileDescriptor)
        Log.i(TAG, "Server socket created at: ${path.absolutePath}")
    }

    /**
     * Start accepting client connections
     * This method launches a coroutine that runs indefinitely
     */
    fun start() {
        if (started) {
            Log.w(TAG, "Server already started")
            return
        }
        started = true

        scope.launch {
            Log.i(TAG, "Accept loop started")
            while (isActive) {
                try {
                    // Accept blocking call (runs on IO dispatcher)
                    val clientSocket = localServerSocket.accept()
                    Log.i(TAG, "Client connected")

                    // Create renderer instance
                    val rendererId = nextRendererId++
                    val renderer = VirGLRenderer(rendererId, clientSocket)
                    clients[rendererId] = renderer

                    // Start render thread (not a coroutine - needs fixed EGL context)
                    val thread = Thread(renderer, "VirGL-Renderer-$rendererId")
                    thread.start()

                    Log.i(TAG, "Renderer $rendererId created and thread started")

                } catch (e: IOException) {
                    if (isActive) {
                        Log.e(TAG, "Accept failed", e)
                        // Continue accepting despite errors
                    } else {
                        Log.i(TAG, "Accept loop stopped")
                        break
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error in accept loop", e)
                }
            }
            Log.i(TAG, "Accept loop finished")
        }
    }

    /**
     * Stop the server and clean up resources
     */
    fun stop() {
        if (!started) {
            return
        }

        Log.i(TAG, "Stopping server...")
        scope.cancel()
        started = false

        // Close all client connections
        clients.forEach { id, renderer ->
            try {
                renderer.close()
                Log.d(TAG, "Closed renderer $id")
            } catch (e: Exception) {
                Log.e(TAG, "Error closing renderer $id", e)
            }
        }
        clients.clear()

        // Close server socket
        try {
            localServerSocket.close()
            localSocket.close()
            Log.i(TAG, "Server stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing server socket", e)
        }
    }
}