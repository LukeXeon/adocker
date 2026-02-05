package com.github.andock.gpu.virgl

/**
 * JNI Native Methods for VirGL Server
 *
 * These methods are implemented in VirGLNative.cpp
 */
internal object VirGLNative {

    init {
        System.loadLibrary("virgl_jni")
    }

    /**
     * Start vtest server
     * @param socketPath Unix socket path
     * @return 0 on success, negative on error
     */
    @JvmStatic
    external fun nativeStartServer(socketPath: String): Int

    /**
     * Stop vtest server
     */
    @JvmStatic
    external fun nativeStopServer()
}
