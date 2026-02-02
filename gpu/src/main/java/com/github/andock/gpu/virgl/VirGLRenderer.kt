package com.github.andock.gpu.virgl

import android.graphics.SurfaceTexture
import android.net.LocalSocket
import android.util.Log
import android.view.Surface
import java.io.Closeable

/**
 * VirGL Renderer instance - one per client connection
 *
 * Manages:
 * - SurfaceTexture for rendering output
 * - VTest protocol parsing
 * - Native VirGL context lifecycle
 */
class VirGLRenderer(
    val id: Long,
    private val localSocket: LocalSocket,
    private val ctxId: Int = 1 // Default context ID
) : Closeable by localSocket, Runnable {
    companion object {
        private const val TAG = "VirGLRenderer"
    }

    val surfaceTexture = SurfaceTexture(false)
    private val surface = Surface(surfaceTexture)
    private val parser = VTestProtocolParser(
        inputStream = localSocket.inputStream,
        outputStream = localSocket.outputStream,
        rendererId = id
    )

    private val fenceCallbacks = mutableMapOf<Int, () -> Unit>()

    init {
        surfaceTexture.setDefaultBufferSize(1280, 720)
        Log.d(TAG, "Renderer created: id=$id")
    }

    override fun run() {
        Thread.currentThread().name = "VirGL-Renderer-$id"
        Log.d(TAG, "Render thread started for renderer $id")

        try {
            // Create native context with surface
            // Note: capsetId will be set later via VCMD_CONTEXT_INIT, use default for now
            val result = VirGLNative.createContext(id, this, surface)
            if (result < 0) {
                Log.e(TAG, "Failed to create native context: $result")
                return
            }

            // Enter protocol processing loop
            parser.processCommands()

        } catch (e: Exception) {
            Log.e(TAG, "Render loop error", e)
        } finally {
            // Clean up native context
            try {
                VirGLNative.destroyContext(id)
                Log.d(TAG, "Native context destroyed for renderer $id")
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying context", e)
            }

            // Clean up Android resources
            surface.release()
            surfaceTexture.release()
            Log.d(TAG, "Render thread finished for renderer $id")
        }
    }

    /**
     * Called from JNI when a fence is signaled
     * This method is invoked by the native layer via JNI callback
     */
    @Suppress("unused")
    @JvmName("onFenceSignaled")
    private fun onFenceSignaled(fenceId: Int) {
        Log.d(TAG, "Fence signaled: $fenceId")
        fenceCallbacks.remove(fenceId)?.invoke()
    }

    /**
     * Register a callback to be invoked when a fence is signaled
     */
    fun registerFenceCallback(fenceId: Int, callback: () -> Unit) {
        fenceCallbacks[fenceId] = callback
    }
}
