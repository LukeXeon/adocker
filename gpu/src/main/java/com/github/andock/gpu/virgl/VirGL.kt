package com.github.andock.gpu.virgl

import java.nio.ByteBuffer

/**
 * VirGLRenderer JNI Wrapper
 *
 * Low-level JNI bindings for virglrenderer C library.
 * Provides 1:1 mapping to native virglrenderer functions.
 */
object VirGL {

    init {
        System.loadLibrary("virgl_jni")
    }

    // ========== Constants ==========

    // virgl_renderer_init flags
    const val RENDERER_USE_GLES = 1 shl 1
    const val RENDERER_THREAD_SYNC = 1 shl 2

    // ========== Data Classes ==========

    /**
     * Capability set query result
     */
    data class CapSet(val maxVer: Int, val maxSize: Int)

    /**
     * Resource creation arguments
     */
    data class ResourceCreateArgs(
        val handle: Int,
        val target: Int,
        val format: Int,
        val bind: Int,
        val width: Int,
        val height: Int,
        val depth: Int = 1,
        val arraySize: Int = 1,
        val lastLevel: Int = 0,
        val nrSamples: Int = 0,
        val flags: Int = 0
    )

    /**
     * Box region for transfer operations
     */
    data class Box(
        val x: Int,
        val y: Int,
        val z: Int,
        val w: Int,
        val h: Int,
        val d: Int
    )

    // ========== Callback Interface ==========

    /**
     * Callback interface for virglrenderer events.
     * The caller must implement these to manage GL contexts.
     */
    interface Callback {
        /**
         * Called when a fence is signaled.
         * @param fenceId The completed fence ID
         */
        fun onWriteFence(fenceId: Int)

        /**
         * Called when virglrenderer needs to create a GL context.
         * @param scanoutIdx Scanout index (usually 0)
         * @param majorVer Requested GL major version
         * @param minorVer Requested GL minor version
         * @return EGLContext pointer as Long, or 0 on failure
         */
        fun onCreateGLContext(scanoutIdx: Int, majorVer: Int, minorVer: Int): Long

        /**
         * Called when virglrenderer needs to destroy a GL context.
         * @param ctx EGLContext pointer
         */
        fun onDestroyGLContext(ctx: Long)

        /**
         * Called when virglrenderer needs to make a context current.
         * @param scanoutIdx Scanout index
         * @param ctx EGLContext pointer (0 to unbind)
         * @return 0 on success, -1 on failure
         */
        fun onMakeCurrent(scanoutIdx: Int, ctx: Long): Int
    }

    // ========== Native Methods ==========

    /**
     * Initialize virglrenderer.
     *
     * @param flags Renderer flags (e.g., RENDERER_USE_GLES)
     * @param callback Callback implementation for GL context management
     * @return 0 on success, -1 on failure.
     *
     * Note: virglrenderer uses global state. Multiple init() calls
     * will replace the previous initialization.
     */
    @JvmStatic
    external fun init(flags: Int, callback: Callback): Int

    /**
     * Cleanup virglrenderer.
     */
    @JvmStatic
    external fun cleanup()

    /**
     * Create a rendering context.
     *
     * @param ctxId Context ID (unique identifier for this context)
     * @param name Debug name for the context
     * @return 0 on success, non-zero on failure
     */
    @JvmStatic
    external fun contextCreate(ctxId: Int, name: String): Int

    /**
     * Destroy a rendering context.
     *
     * @param ctxId Context ID to destroy
     */
    @JvmStatic
    external fun contextDestroy(ctxId: Int)

    /**
     * Query capability set info.
     *
     * @param set Capability set ID (1 for CAPS, 2 for CAPS2)
     * @return CapSet with max version and size, or null on failure
     */
    @JvmStatic
    external fun getCapSet(set: Int): CapSet?

    /**
     * Fill capability buffer.
     *
     * @param set Capability set ID
     * @param version Version from getCapSet()
     * @param buffer Direct ByteBuffer to fill with capability data
     * @return 0 on success, -1 on failure
     */
    @JvmStatic
    external fun fillCaps(set: Int, version: Int, buffer: ByteBuffer): Int

    /**
     * Create a resource.
     *
     * @param args Resource creation arguments
     * @return Resource handle on success, 0 on failure
     */
    @JvmStatic
    external fun resourceCreate(args: ResourceCreateArgs): Int

    /**
     * Unreference (delete) a resource.
     *
     * @param handle Resource handle to delete
     */
    @JvmStatic
    external fun resourceUnref(handle: Int)

    /**
     * Attach a resource to a context.
     *
     * @param ctxId Context ID
     * @param resHandle Resource handle
     */
    @JvmStatic
    external fun ctxAttachResource(ctxId: Int, resHandle: Int)

    /**
     * Detach a resource from a context.
     *
     * @param ctxId Context ID
     * @param resHandle Resource handle
     */
    @JvmStatic
    external fun ctxDetachResource(ctxId: Int, resHandle: Int)

    /**
     * Submit GPU commands.
     *
     * @param buffer Command buffer (must be direct ByteBuffer, 4-byte aligned)
     * @param ctxId Context ID
     * @return 0 on success, non-zero on failure
     */
    @JvmStatic
    external fun submitCmd(buffer: ByteBuffer, ctxId: Int): Int

    /**
     * Create a fence for synchronization.
     *
     * @param fenceId Fence identifier
     * @param ctxId Context ID
     * @return 0 on success, non-zero on failure
     */
    @JvmStatic
    external fun createFence(fenceId: Int, ctxId: Int): Int

    /**
     * Read data from a resource (GPU to CPU transfer).
     *
     * @param handle Resource handle
     * @param ctxId Context ID
     * @param level Mipmap level
     * @param stride Row stride in bytes
     * @param layerStride Layer stride for 3D textures
     * @param box Region to read
     * @param offset Offset in bytes
     * @param buffer Direct ByteBuffer to receive data
     * @return 0 on success, non-zero on failure
     */
    @JvmStatic
    external fun transferGet(
        handle: Int,
        ctxId: Int,
        level: Int,
        stride: Int,
        layerStride: Int,
        box: Box,
        offset: Long,
        buffer: ByteBuffer
    ): Int

    /**
     * Write data to a resource (CPU to GPU transfer).
     *
     * @param handle Resource handle
     * @param ctxId Context ID
     * @param level Mipmap level
     * @param stride Row stride in bytes
     * @param layerStride Layer stride for 3D textures
     * @param box Region to write
     * @param offset Offset in bytes
     * @param buffer Direct ByteBuffer containing data to write
     * @return 0 on success, non-zero on failure
     */
    @JvmStatic
    external fun transferPut(
        handle: Int,
        ctxId: Int,
        level: Int,
        stride: Int,
        layerStride: Int,
        box: Box,
        offset: Long,
        buffer: ByteBuffer
    ): Int

    /**
     * Poll for completed fences.
     * Call this periodically to trigger onWriteFence callbacks.
     */
    @JvmStatic
    external fun poll()
}
