package com.github.andock.gpu.virgl

import android.util.Log
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * VTest Protocol Parser
 *
 * Parses the vtest protocol (v3/v4) and dispatches commands to either
 * Kotlin handlers (simple commands) or JNI (complex commands).
 *
 * Protocol format:
 * Header: [length_dw: uint32][cmd_id: uint32]
 * Data: [uint32 array of length_dw - 2]
 */
class VTestProtocolParser(
    private val inputStream: InputStream,
    private val outputStream: OutputStream,
    private val rendererId: Long
) {
    companion object {
        private const val TAG = "VTestProtocolParser"
        private const val HEADER_SIZE = 8 // 2 * uint32
        private const val VTEST_PROTOCOL_VERSION = 4
    }

    private var protocolVersion = 0 // Negotiated version
    private var contextInitialized = false

    /**
     * Main command processing loop
     * Runs until the connection is closed or an error occurs
     */
    fun processCommands() {
        try {
            while (true) {
                val header = readHeader() ?: break
                val lengthDw = header.first
                val cmdId = header.second

                Log.d(TAG, "Command: id=$cmdId, length_dw=$lengthDw")

                when (cmdId) {
                    // Simple commands (Kotlin layer)
                    VTestCommand.VCMD_CREATE_RENDERER -> handleCreateRenderer()
                    VTestCommand.VCMD_PING_PROTOCOL_VERSION -> handlePingProtocolVersion()
                    VTestCommand.VCMD_PROTOCOL_VERSION -> handleProtocolVersion()
                    VTestCommand.VCMD_GET_CAPS -> handleGetCaps()
                    VTestCommand.VCMD_GET_CAPS2 -> handleGetCaps2()

                    // Complex commands (JNI layer)
                    VTestCommand.VCMD_CONTEXT_INIT -> handleContextInit(lengthDw)
                    VTestCommand.VCMD_RESOURCE_CREATE_BLOB -> handleResourceCreateBlob(lengthDw)
                    VTestCommand.VCMD_RESOURCE_UNREF -> handleResourceUnref(lengthDw)
                    VTestCommand.VCMD_SUBMIT_CMD -> handleSubmitCmd(lengthDw)
                    VTestCommand.VCMD_SUBMIT_CMD2 -> handleSubmitCmd2(lengthDw)
                    VTestCommand.VCMD_SYNC_CREATE -> handleSyncCreate(lengthDw)
                    VTestCommand.VCMD_SYNC_WRITE -> handleSyncWrite(lengthDw)
                    VTestCommand.VCMD_SYNC_WAIT -> handleSyncWait(lengthDw)
                    VTestCommand.VCMD_SYNC_UNREF -> handleSyncUnref(lengthDw)
                    VTestCommand.VCMD_TRANSFER_PUT2 -> handleTransferPut2(lengthDw)
                    VTestCommand.VCMD_TRANSFER_GET2 -> handleTransferGet2(lengthDw)

                    else -> {
                        Log.w(TAG, "Unknown command: $cmdId")
                        skipCommand(lengthDw)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Protocol error", e)
            throw e
        }
    }

    /**
     * Read protocol header (8 bytes: length_dw + cmd_id)
     * @return Pair of (length_dw, cmd_id) or null if EOF
     */
    private fun readHeader(): Pair<Int, Int>? {
        val header = ByteArray(HEADER_SIZE)
        val read = inputStream.read(header)
        if (read != HEADER_SIZE) {
            return null
        }

        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        val lengthDw = buffer.getInt()
        val cmdId = buffer.getInt()
        return Pair(lengthDw, cmdId)
    }

    /**
     * Read uint32 array from stream
     */
    private fun readUint32Array(count: Int): IntArray {
        val buffer = ByteArray(count * 4)
        inputStream.read(buffer)
        val bb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
        return IntArray(count) { bb.getInt() }
    }

    /**
     * Read a single uint32
     */
    private fun readUint32(): Int {
        return readUint32Array(1)[0]
    }

    /**
     * Skip command data (unimplemented commands)
     */
    private fun skipCommand(lengthDw: Int) {
        val dataSize = (lengthDw - 2) * 4
        if (dataSize > 0) {
            inputStream.skip(dataSize.toLong())
        }
    }

    /**
     * Write response header and data
     */
    private fun writeResponse(cmdId: Int, data: IntArray = intArrayOf()) {
        val lengthDw = 2 + data.size
        val buffer = ByteBuffer.allocate(lengthDw * 4).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(lengthDw)
        buffer.putInt(cmdId)
        data.forEach { buffer.putInt(it) }
        outputStream.write(buffer.array())
        outputStream.flush()
    }

    // ========== Command Handlers (Simple - Kotlin Layer) ==========

    private fun handleCreateRenderer() {
        // Read debug name (32 bytes)
        val nameBytes = ByteArray(32)
        inputStream.read(nameBytes)
        val name = String(nameBytes).trimEnd('\u0000')
        Log.d(TAG, "CREATE_RENDERER: name=$name")
        // No response needed
    }

    private fun handlePingProtocolVersion() {
        Log.d(TAG, "PING_PROTOCOL_VERSION")
        writeResponse(VTestCommand.VCMD_PROTOCOL_VERSION, intArrayOf(VTEST_PROTOCOL_VERSION))
    }

    private fun handleProtocolVersion() {
        protocolVersion = readUint32()
        Log.d(TAG, "PROTOCOL_VERSION: client=$protocolVersion, server=$VTEST_PROTOCOL_VERSION")
        val negotiated = minOf(protocolVersion, VTEST_PROTOCOL_VERSION)
        writeResponse(VTestCommand.VCMD_PROTOCOL_VERSION, intArrayOf(negotiated))
    }

    private fun handleGetCaps() {
        Log.d(TAG, "GET_CAPS")
        // Get capabilities from native layer
        val caps = VirGLNative.getCaps()
        val capsArray = IntArray(caps.remaining() / 4)
        caps.asIntBuffer().get(capsArray)
        writeResponse(VTestCommand.VCMD_GET_CAPS, capsArray)
    }

    private fun handleGetCaps2() {
        Log.d(TAG, "GET_CAPS2")
        // Same as GET_CAPS for now
        handleGetCaps()
    }

    // ========== Command Handlers (Complex - JNI Layer) ==========

    // Stage 3: Context Creation
    private fun handleContextInit(lengthDw: Int) {
        val params = readUint32Array(lengthDw - 2)
        val capsetId = params[0]
        Log.d(TAG, "CONTEXT_INIT: capsetId=$capsetId")

        // Initialize virgl context with the specified capset
        val result = VirGLNative.initContext(rendererId, capsetId)
        if (result < 0) {
            Log.e(TAG, "Failed to initialize context with capsetId: $result")
        } else {
            contextInitialized = true
            Log.i(TAG, "Context initialized successfully with capsetId=$capsetId")
        }
    }

    // Stage 4: Blob Resource Management
    private fun handleResourceCreateBlob(lengthDw: Int) {
        val params = readUint32Array(lengthDw - 2)
        val resId = params[0]
        val blobType = params[1]
        val blobFlags = params[2]
        val blobId = (params[3].toLong() and 0xFFFFFFFFL) or ((params[4].toLong() and 0xFFFFFFFFL) shl 32)
        val size = (params[5].toLong() and 0xFFFFFFFFL) or ((params[6].toLong() and 0xFFFFFFFFL) shl 32)

        Log.d(TAG, "RESOURCE_CREATE_BLOB: resId=$resId, type=$blobType, flags=$blobFlags, blobId=$blobId, size=$size")

        val blobArgs = VTestCommand.BlobArgs(
            resId = resId,
            blobType = blobType,
            blobFlags = blobFlags,
            blobId = blobId,
            size = size
        )

        val result = VirGLNative.createResourceBlob(rendererId, blobArgs)
        if (result < 0) {
            Log.e(TAG, "Failed to create blob resource: $result")
        }

        // If EXPORT flag is set, export the blob and send FD
        if ((blobFlags and VTestCommand.BLOB_FLAG_EXPORT) != 0) {
            val fd = VirGLNative.exportResourceBlob(rendererId, resId)
            if (fd >= 0) {
                // Send FD via ancillary data
                try {
                    // TODO: Implement FD sending via LocalSocket
                    Log.d(TAG, "Exported blob fd=$fd for resId=$resId")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send FD", e)
                }
            }
        }
    }

    private fun handleResourceUnref(lengthDw: Int) {
        val resId = readUint32()
        Log.d(TAG, "RESOURCE_UNREF: resId=$resId")

        val result = VirGLNative.destroyResource(rendererId, resId)
        if (result < 0) {
            Log.e(TAG, "Failed to destroy resource: $result")
        }
    }

    // Stage 5: Command Submission
    private fun handleSubmitCmd(lengthDw: Int) {
        val cmdSize = (lengthDw - 2) * 4 // Bytes
        val cmdBuffer = ByteArray(cmdSize)
        inputStream.read(cmdBuffer)

        Log.d(TAG, "SUBMIT_CMD: size=$cmdSize bytes")

        val directBuffer = ByteBuffer.allocateDirect(cmdSize)
        directBuffer.order(ByteOrder.LITTLE_ENDIAN)
        directBuffer.put(cmdBuffer)
        directBuffer.flip()

        val result = VirGLNative.submitCmd(rendererId, directBuffer)
        if (result < 0) {
            Log.e(TAG, "Failed to submit command: $result")
        }
    }

    private fun handleSubmitCmd2(lengthDw: Int) {
        // Submit command v2 with sync objects
        val batchHeader = readUint32Array(8)
        val flags = batchHeader[0]
        val ringIdx = batchHeader[1]
        val cmdSize = batchHeader[2]
        val numInSyncs = batchHeader[3]
        // batchHeader[4-7] reserved

        Log.d(TAG, "SUBMIT_CMD2: cmdSize=$cmdSize, numInSyncs=$numInSyncs, flags=$flags, ringIdx=$ringIdx")

        // Read command data
        val cmdBuffer = ByteArray(cmdSize)
        inputStream.read(cmdBuffer)

        // Read input sync objects
        val inSyncIds = if (numInSyncs > 0) {
            IntArray(numInSyncs) { readUint32() }
        } else {
            intArrayOf()
        }

        val directBuffer = ByteBuffer.allocateDirect(cmdSize)
        directBuffer.order(ByteOrder.LITTLE_ENDIAN)
        directBuffer.put(cmdBuffer)
        directBuffer.flip()

        val args = VTestCommand.SubmitCmdArgs(
            flags = flags,
            ringIdx = ringIdx,
            cmdSize = cmdSize,
            numInSyncs = numInSyncs,
            inSyncIds = inSyncIds
        )

        val result = VirGLNative.submitCmd2(rendererId, directBuffer, args)
        if (result < 0) {
            Log.e(TAG, "Failed to submit command2: $result")
        }
    }

    // Stage 6: Synchronization
    private fun handleSyncCreate(lengthDw: Int) {
        val params = readUint32Array(lengthDw - 2)
        val syncId = params[0]
        val initialValue = if (params.size > 1) {
            (params[1].toLong() and 0xFFFFFFFFL) or ((params[2].toLong() and 0xFFFFFFFFL) shl 32)
        } else {
            0L
        }

        Log.d(TAG, "SYNC_CREATE: syncId=$syncId, initialValue=$initialValue")

        val result = VirGLNative.createSync(rendererId, syncId, initialValue)
        if (result < 0) {
            Log.e(TAG, "Failed to create sync: $result")
        }
    }

    private fun handleSyncWrite(lengthDw: Int) {
        val params = readUint32Array(lengthDw - 2)
        val syncId = params[0]
        val value = (params[1].toLong() and 0xFFFFFFFFL) or ((params[2].toLong() and 0xFFFFFFFFL) shl 32)

        Log.d(TAG, "SYNC_WRITE: syncId=$syncId, value=$value")

        val result = VirGLNative.writeSync(rendererId, syncId, value)
        if (result < 0) {
            Log.e(TAG, "Failed to write sync: $result")
        }
    }

    private fun handleSyncWait(lengthDw: Int) {
        val params = readUint32Array(lengthDw - 2)
        val numSyncs = params[0]

        val syncIds = IntArray(numSyncs)
        val syncValues = LongArray(numSyncs)

        for (i in 0 until numSyncs) {
            val base = 1 + i * 3
            syncIds[i] = params[base]
            syncValues[i] = (params[base + 1].toLong() and 0xFFFFFFFFL) or
                           ((params[base + 2].toLong() and 0xFFFFFFFFL) shl 32)
        }

        Log.d(TAG, "SYNC_WAIT: numSyncs=$numSyncs")

        val result = VirGLNative.syncWait(rendererId, syncIds, syncValues, timeoutNs = 1_000_000_000L)

        // Send response with wait result (fd or status)
        writeResponse(VTestCommand.VCMD_SYNC_WAIT, intArrayOf(result))
    }

    private fun handleSyncUnref(lengthDw: Int) {
        val syncId = readUint32()
        Log.d(TAG, "SYNC_UNREF: syncId=$syncId")

        val result = VirGLNative.destroySync(rendererId, syncId)
        if (result < 0) {
            Log.e(TAG, "Failed to destroy sync: $result")
        }
    }

    // Stage 7: Transfer Operations
    private fun handleTransferPut2(lengthDw: Int) {
        val params = readUint32Array(lengthDw - 2)
        val resId = params[0]
        val level = params[1]
        val x = params[2]
        val y = params[3]
        val z = params[4]
        val width = params[5]
        val height = params[6]
        val depth = params[7]
        val stride = params[8]
        val layerStride = params[9]
        val dataSize = params[10]

        Log.d(TAG, "TRANSFER_PUT2: resId=$resId, level=$level, size=${width}x${height}x${depth}, dataSize=$dataSize")

        // Read transfer data
        val data = ByteArray(dataSize)
        inputStream.read(data)

        val directBuffer = ByteBuffer.allocateDirect(dataSize)
        directBuffer.put(data)
        directBuffer.flip()

        val result = VirGLNative.transferPut(
            rendererId, resId, level,
            x, y, z, width, height, depth,
            stride, layerStride, directBuffer
        )
        if (result < 0) {
            Log.e(TAG, "Failed to transfer put: $result")
        }
    }

    private fun handleTransferGet2(lengthDw: Int) {
        val params = readUint32Array(lengthDw - 2)
        val resId = params[0]
        val level = params[1]
        val x = params[2]
        val y = params[3]
        val z = params[4]
        val width = params[5]
        val height = params[6]
        val depth = params[7]
        val stride = params[8]
        val layerStride = params[9]

        Log.d(TAG, "TRANSFER_GET2: resId=$resId, level=$level, size=${width}x${height}x${depth}")

        val dataSize = stride * height * depth
        val directBuffer = ByteBuffer.allocateDirect(dataSize)

        val result = VirGLNative.transferGet(
            rendererId, resId, level,
            x, y, z, width, height, depth,
            stride, layerStride, directBuffer
        )

        if (result < 0) {
            Log.e(TAG, "Failed to transfer get: $result")
        } else {
            // Send data back to client
            directBuffer.flip()
            val data = ByteArray(directBuffer.remaining())
            directBuffer.get(data)
            outputStream.write(data)
            outputStream.flush()
        }
    }
}
