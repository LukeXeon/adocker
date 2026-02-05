package com.github.andock.gpu.demo.client

import java.io.Closeable

/**
 * VirGL Client Interface
 *
 * Provides high-level API for vtest protocol communication.
 */
interface VirGLClient : Closeable {
    /**
     * Handshake protocol version with server
     */
    suspend fun handshake(): Result<Unit>

    /**
     * Query capabilities from server
     * @param capsetId Capability set ID
     * @return Capability data if available
     */
    suspend fun queryCaps(capsetId: UInt): Result<ByteArray?>

    /**
     * Initialize virgl context
     * @param capsetId Capability set ID to use
     */
    suspend fun initContext(capsetId: UInt): Result<Unit>

    /**
     * Create a blob resource
     * @param blobType Blob type (HOST3D, GUEST, etc.)
     * @param blobFlags Blob flags
     * @param size Resource size in bytes
     * @param blobId Unique blob identifier
     * @return Resource ID allocated by server
     */
    suspend fun createBlobResource(
        blobType: UInt,
        blobFlags: UInt,
        size: ULong,
        blobId: ULong
    ): Result<UInt>

    /**
     * Submit virgl commands to server
     * @param commands Command buffer (array of UInts)
     */
    suspend fun submitCommands(commands: UIntArray): Result<Unit>

    /**
     * Transfer data to resource (upload)
     * @param resourceId Target resource ID
     * @param level Mipmap level
     * @param x X offset
     * @param y Y offset
     * @param z Z offset
     * @param width Box width
     * @param height Box height
     * @param depth Box depth
     * @param data Data to upload
     * @param offset Offset in resource
     */
    suspend fun transferPut(
        resourceId: UInt,
        level: UInt,
        x: UInt, y: UInt, z: UInt,
        width: UInt, height: UInt, depth: UInt,
        data: ByteArray,
        offset: ULong = 0u
    ): Result<Unit>

    /**
     * Transfer data from resource (download)
     * @param resourceId Source resource ID
     * @param level Mipmap level
     * @param x X offset
     * @param y Y offset
     * @param z Z offset
     * @param width Box width
     * @param height Box height
     * @param depth Box depth
     * @param dataSize Expected data size
     * @param offset Offset in resource
     * @return Downloaded data
     */
    suspend fun transferGet(
        resourceId: UInt,
        level: UInt,
        x: UInt, y: UInt, z: UInt,
        width: UInt, height: UInt, depth: UInt,
        dataSize: UInt,
        offset: ULong = 0u
    ): Result<ByteArray>

    /**
     * Unreference (delete) a resource
     * @param resourceId Resource ID to delete
     */
    suspend fun unrefResource(resourceId: UInt): Result<Unit>

    /**
     * Check if client is connected
     */
    val isConnected: Boolean
}
