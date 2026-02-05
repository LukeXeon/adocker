package com.github.andock.gpu.demo

import android.content.Context
import android.util.AttributeSet
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.github.andock.gpu.demo.client.VirGLClient
import com.github.andock.gpu.demo.client.VirGLClientImpl
import com.github.andock.gpu.demo.scenes.RenderScene
import com.github.andock.gpu.demo.scenes.TriangleScene
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * VirGL SurfaceView
 *
 * Custom SurfaceView for rendering VirGL output.
 * Connects to vtest server via Unix socket and attaches the surface for rendering.
 *
 * Usage:
 * ```kotlin
 * val view = VirGLSurfaceView(context)
 * view.setServerSocketPath("/path/to/virgl-vtest.sock")
 * view.setScene(TriangleScene())
 * ```
 */
class VirGLSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    private var client: VirGLClient? = null
    private var scene: RenderScene? = null
    private var socketPath: String? = null
    private var renderScope: CoroutineScope? = null
    private var renderJob: Job? = null

    init {
        holder.addCallback(this)
    }

    /**
     * Set vtest server socket path
     */
    fun setServerSocketPath(path: String) {
        socketPath = path
    }

    /**
     * Set rendering scene
     */
    fun setScene(newScene: RenderScene) {
        scene = newScene
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val path = socketPath ?: run {
            Timber.e("Socket path not set")
            return
        }

        val currentScene = scene ?: TriangleScene().also { scene = it }

        // Create coroutine scope for rendering
        renderScope = CoroutineScope(Dispatchers.Default)

        // Connect to server and start rendering
        renderScope?.launch {
            try {
                // 1. Connect to vtest server
                Timber.i("Connecting to vtest server: $path")
                val virglClient = VirGLClientImpl(path)
                virglClient.connect().getOrThrow()
                client = virglClient

                // 2. Handshake
                virglClient.handshake().getOrThrow()
                Timber.i("Handshake successful")

                // 3. Query capabilities (optional, for logging)
                virglClient.queryCaps(2u).onSuccess { caps ->
                    Timber.d("Capabilities received: ${caps?.size ?: 0} bytes")
                }

                // 4. Attach surface for rendering (native call)
                // Note: We need a way to get the client FD for native attachment
                // For now, just log that we're ready
                Timber.i("Client connected, ready to render")

                // 5. Initialize scene
                val width = holder.surfaceFrame.width()
                val height = holder.surfaceFrame.height()
                currentScene.init(virglClient, width, height).getOrThrow()
                Timber.i("Scene initialized: ${currentScene.name}")

                // 6. Start render loop
                startRenderLoop(currentScene)

            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize VirGL rendering")
                cleanup()
            }
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Timber.d("Surface changed: ${width}x$height")
        renderScope?.launch {
            scene?.resize(width, height)
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Timber.i("Surface destroyed, cleaning up")
        cleanup()
    }

    private fun startRenderLoop(currentScene: RenderScene) {
        renderJob = renderScope?.launch {
            var lastFrameTime = System.nanoTime()

            while (isActive) {
                try {
                    val currentTime = System.nanoTime()
                    val deltaTime = (currentTime - lastFrameTime) / 1_000_000_000f
                    lastFrameTime = currentTime

                    // Render frame
                    currentScene.render(deltaTime).onFailure { e ->
                        Timber.e(e, "Render error")
                    }

                    // Target 60 FPS (~16.7ms per frame)
                    delay(16)

                } catch (e: Exception) {
                    Timber.e(e, "Render loop error")
                    break
                }
            }

            Timber.i("Render loop exited")
        }
    }

    private fun cleanup() {
        // Stop render loop
        renderJob?.cancel()
        renderJob = null

        // Cleanup scene
        renderScope?.launch {
            try {
                scene?.cleanup()
                Timber.d("Scene cleaned up")
            } catch (e: Exception) {
                Timber.e(e, "Scene cleanup error")
            }

            // Close client connection
            try {
                client?.close()
                client = null
                Timber.d("Client connection closed")
            } catch (e: Exception) {
                Timber.e(e, "Client close error")
            }

            // Cancel scope
            renderScope?.cancel()
            renderScope = null
        }
    }

    /**
     * Attach ANativeWindow to a client for rendering
     * @param clientFd Client file descriptor
     * @param surface Surface to attach
     */
    private external fun nativeAttachSurface(clientFd: Int, surface: Surface)

    /**
     * Detach surface from client
     * @param clientFd Client file descriptor
     */
    private external fun nativeDetachSurface(clientFd: Int)

    companion object {
        init {
            System.loadLibrary("virgl_jni")
        }
    }
}
