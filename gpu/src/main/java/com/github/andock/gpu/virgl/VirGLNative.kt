package com.github.andock.gpu.virgl

import android.view.Surface
import java.nio.ByteBuffer

/**
 * JNI interface to virglrenderer native library
 *
 * All methods are thread-safe and can be called from any thread.
 * The native layer manages thread-local JNIEnv and EGL contexts.
 */
object VirGLNative {

    /**
     * Initialize virglrenderer globally (call once at startup)
     *
     * @param flags Renderer flags (VIRGL_RENDERER_USE_EGL | VIRGL_RENDERER_USE_EXTERNAL_BLOB)
     * @return 0 on success, negative error code on failure
     */
    external fun initRenderer(flags: Int): Int

    /**
     * Create a rendering context for a client (pre-initialization)
     * This creates the EGL context and surface, but doesn't create virgl context yet
     *
     * @param rendererId Unique ID for this renderer instance
     * @param renderer VirGLRenderer instance (for fence callbacks)
     * @param surface Android Surface to render to (from SurfaceTexture)
     * @return 0 on success, negative error code on failure
     */
    external fun createContext(rendererId: Long, renderer: VirGLRenderer, surface: Surface): Int

    /**
     * Initialize virgl context with capset ID (called via VCMD_CONTEXT_INIT)
     *
     * @param rendererId Renderer instance ID
     * @param capsetId Capset ID (VIRTGPU_DRM_CAPSET_VIRGL2, etc.)
     * @return 0 on success, negative error code on failure
     */
    external fun initContext(rendererId: Long, capsetId: Int): Int

    /**
     * Destroy a rendering context
     *
     * @param rendererId Renderer instance ID
     */
    external fun destroyContext(rendererId: Long)

    /**
     * Create a blob resource
     *
     * @param rendererId Renderer instance ID
     * @param args Blob creation arguments
     * @return Resource ID on success, negative error code on failure
     */
    external fun createResourceBlob(rendererId: Long, args: BlobArgs): Int

    /**
     * Export a blob resource as file descriptor
     *
     * @param rendererId Renderer instance ID
     * @param resId Resource ID to export
     * @return File descriptor on success, negative error code on failure
     */
    external fun exportResourceBlob(rendererId: Long, resId: Int): Int

    /**
     * Destroy a resource
     *
     * @param rendererId Renderer instance ID
     * @param resId Resource ID to destroy
     * @return 0 on success, negative error code on failure
     */
    external fun destroyResource(rendererId: Long, resId: Int): Int

    /**
     * Submit rendering commands (simple version)
     *
     * @param rendererId Renderer instance ID
     * @param cmdBuffer Direct ByteBuffer containing commands
     * @return 0 on success, negative error code on failure
     */
    external fun submitCmd(
        rendererId: Long,
        cmdBuffer: ByteBuffer
    ): Int

    /**
     * Submit rendering commands v2 (with sync objects)
     *
     * @param rendererId Renderer instance ID
     * @param cmdBuffer Direct ByteBuffer containing commands
     * @param args Submit command arguments (flags, ring, syncs)
     * @return 0 on success, negative error code on failure
     */
    external fun submitCmd2(
        rendererId: Long,
        cmdBuffer: ByteBuffer,
        args: VTestCommand.SubmitCmdArgs
    ): Int

    /**
     * Create a sync object
     *
     * @param rendererId Renderer instance ID
     * @param syncId Sync object ID (from protocol)
     * @param initialValue Initial value of the sync object
     * @return 0 on success, negative error code on failure
     */
    external fun createSync(rendererId: Long, syncId: Int, initialValue: Long): Int

    /**
     * Wait for sync objects to reach specified values
     *
     * @param rendererId Renderer instance ID
     * @param syncIds Array of sync IDs to wait for
     * @param values Array of values to wait for
     * @param timeoutNs Timeout in nanoseconds (0 = no timeout)
     * @return 0 on success, -ETIMEDOUT on timeout, negative error code on failure
     */
    external fun syncWait(
        rendererId: Long,
        syncIds: IntArray,
        values: LongArray,
        timeoutNs: Long
    ): Int

    /**
     * Update a sync object's value
     *
     * @param rendererId Renderer instance ID
     * @param syncId Sync ID to update
     * @param value New value
     * @return 0 on success, negative error code on failure
     */
    external fun writeSync(rendererId: Long, syncId: Int, value: Long): Int

    /**
     * Destroy a sync object
     *
     * @param rendererId Renderer instance ID
     * @param syncId Sync ID to destroy
     * @return 0 on success, negative error code on failure
     */
    external fun destroySync(rendererId: Long, syncId: Int): Int

    /**
     * Get renderer capabilities (caps)
     *
     * @return ByteBuffer containing capabilities data
     */
    external fun getCaps(): ByteBuffer

    /**
     * Transfer data to resource (upload)
     *
     * @param rendererId Renderer instance ID
     * @param resId Resource ID
     * @param level Mipmap level
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param width Width
     * @param height Height
     * @param depth Depth
     * @param stride Row stride in bytes
     * @param layerStride Layer stride in bytes
     * @param data Data to transfer
     * @return 0 on success, negative error code on failure
     */
    external fun transferPut(
        rendererId: Long,
        resId: Int,
        level: Int,
        x: Int, y: Int, z: Int,
        width: Int, height: Int, depth: Int,
        stride: Int,
        layerStride: Int,
        data: ByteBuffer
    ): Int

    /**
     * Transfer data from resource (download)
     *
     * @param rendererId Renderer instance ID
     * @param resId Resource ID
     * @param level Mipmap level
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param width Width
     * @param height Height
     * @param depth Depth
     * @param stride Row stride in bytes
     * @param layerStride Layer stride in bytes
     * @param data Buffer to receive data
     * @return 0 on success, negative error code on failure
     */
    external fun transferGet(
        rendererId: Long,
        resId: Int,
        level: Int,
        x: Int, y: Int, z: Int,
        width: Int, height: Int, depth: Int,
        stride: Int,
        layerStride: Int,
        data: ByteBuffer
    ): Int
}
