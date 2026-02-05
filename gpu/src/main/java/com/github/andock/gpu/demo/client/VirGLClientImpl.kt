package com.github.andock.gpu.demo.client

import android.net.LocalSocket
import android.net.LocalSocketAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * VirGL Client Implementation
 *
 * Implements vtest protocol over Unix domain socket.
 * Thread-safe through coroutine dispatchers.
 */
class VirGLClientImpl(private val socketPath: String) : VirGLClient {

    private var socket: LocalSocket? = null
    private val mutex = Any()

    override val isConnected: Boolean
        get() = socket?.isConnected == true

    /**
     * Connect to vtest server
     */
    suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            synchronized(mutex) {
                if (isConnected) {
                    Timber.w("Already connected")
                    return@runCatching
                }

                val sock = LocalSocket()
                sock.connect(LocalSocketAddress(socketPath, LocalSocketAddress.Namespace.FILESYSTEM))
                socket = sock
                Timber.i("Connected to vtest server: $socketPath")
            }
        }
    }

    override suspend fun handshake(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Send protocol version
            val request = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN).apply {
                putInt(1) // length in dwords
                putInt(VTestProtocol.VCMD_PROTOCOL_VERSION.toInt())
                putInt(VTestProtocol.VTEST_PROTOCOL_VERSION.toInt())
            }
            writeData(request.array())

            // Read response
            val response = readResponse()
            val serverVersion = response.getInt(8)
            Timber.i("Server protocol version: $serverVersion")

            if (serverVersion.toUInt() != VTestProtocol.VTEST_PROTOCOL_VERSION) {
                throw IOException("Protocol version mismatch: server=$serverVersion, client=${VTestProtocol.VTEST_PROTOCOL_VERSION}")
            }
        }
    }

    override suspend fun queryCaps(capsetId: UInt): Result<ByteArray?> = withContext(Dispatchers.IO) {
        runCatching {
            // Send GET_CAPS2 request
            val request = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN).apply {
                putInt(2) // length in dwords
                putInt(VTestProtocol.VCMD_GET_CAPS2.toInt())
                putInt(capsetId.toInt())
                putInt(0) // version
            }
            writeData(request.array())

            // Read response
            val response = readResponse()
            val valid = response.getInt(8)

            if (valid == 1) {
                // Extract capability data (skip length and command)
                val capsData = ByteArray(response.remaining())
                response.get(capsData)
                Timber.d("Got capabilities: capset=$capsetId, size=${capsData.size}")
                capsData
            } else {
                Timber.w("No capabilities available for capset $capsetId")
                null
            }
        }
    }

    override suspend fun initContext(capsetId: UInt): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Send CONTEXT_INIT request (no response expected)
            val request = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN).apply {
                putInt(1) // length in dwords
                putInt(VTestProtocol.VCMD_CONTEXT_INIT.toInt())
                putInt(capsetId.toInt())
            }
            writeData(request.array())
            Timber.i("Context initialized with capset $capsetId")
        }
    }

    override suspend fun createBlobResource(
        blobType: UInt,
        blobFlags: UInt,
        size: ULong,
        blobId: ULong
    ): Result<UInt> = withContext(Dispatchers.IO) {
        runCatching {
            // Send RESOURCE_CREATE_BLOB request
            val request = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN).apply {
                putInt(6) // length in dwords
                putInt(VTestProtocol.VCMD_RESOURCE_CREATE_BLOB.toInt())
                putInt(blobType.toInt())
                putInt(blobFlags.toInt())
                putInt((size and 0xFFFFFFFFu).toInt()) // size low
                putInt((size shr 32).toInt()) // size high
                putInt((blobId and 0xFFFFFFFFu).toInt()) // blob_id low
                putInt((blobId shr 32).toInt()) // blob_id high
            }
            writeData(request.array())

            // Read response (resource ID)
            val response = readResponse()
            val resourceId = response.getInt(8).toUInt()
            Timber.d("Created blob resource: id=$resourceId, size=$size")
            resourceId
        }
    }

    override suspend fun submitCommands(commands: UIntArray): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Send SUBMIT_CMD request (no response expected)
            val request = ByteBuffer.allocate(8 + commands.size * 4).order(ByteOrder.LITTLE_ENDIAN).apply {
                putInt(commands.size) // length in dwords
                putInt(VTestProtocol.VCMD_SUBMIT_CMD.toInt())
                commands.forEach { putInt(it.toInt()) }
            }
            writeData(request.array())
            Timber.d("Submitted ${commands.size} command dwords")
        }
    }

    override suspend fun transferPut(
        resourceId: UInt,
        level: UInt,
        x: UInt, y: UInt, z: UInt,
        width: UInt, height: UInt, depth: UInt,
        data: ByteArray,
        offset: ULong
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Send TRANSFER_PUT2 request (no response expected)
            val request = ByteBuffer.allocate(48 + data.size).order(ByteOrder.LITTLE_ENDIAN).apply {
                putInt(10) // header length in dwords
                putInt(VTestProtocol.VCMD_TRANSFER_PUT2.toInt())
                putInt(resourceId.toInt())
                putInt(level.toInt())
                putInt(x.toInt())
                putInt(y.toInt())
                putInt(z.toInt())
                putInt(width.toInt())
                putInt(height.toInt())
                putInt(depth.toInt())
                putInt(data.size)
                putInt((offset and 0xFFFFFFFFu).toInt()) // offset low (64-bit would need high part too)
                put(data)
            }
            writeData(request.array())
            Timber.d("Transfer PUT: res=$resourceId, size=${data.size}")
        }
    }

    override suspend fun transferGet(
        resourceId: UInt,
        level: UInt,
        x: UInt, y: UInt, z: UInt,
        width: UInt, height: UInt, depth: UInt,
        dataSize: UInt,
        offset: ULong
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            // Send TRANSFER_GET2 request
            val request = ByteBuffer.allocate(48).order(ByteOrder.LITTLE_ENDIAN).apply {
                putInt(10) // length in dwords
                putInt(VTestProtocol.VCMD_TRANSFER_GET2.toInt())
                putInt(resourceId.toInt())
                putInt(level.toInt())
                putInt(x.toInt())
                putInt(y.toInt())
                putInt(z.toInt())
                putInt(width.toInt())
                putInt(height.toInt())
                putInt(depth.toInt())
                putInt(dataSize.toInt())
                putInt((offset and 0xFFFFFFFFu).toInt())
            }
            writeData(request.array())

            // Read response
            val response = readResponse()
            val data = ByteArray(response.remaining())
            response.get(data)
            Timber.d("Transfer GET: res=$resourceId, size=${data.size}")
            data
        }
    }

    override suspend fun unrefResource(resourceId: UInt): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Send RESOURCE_UNREF request (no response expected)
            val request = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN).apply {
                putInt(1) // length in dwords
                putInt(VTestProtocol.VCMD_RESOURCE_UNREF.toInt())
                putInt(resourceId.toInt())
            }
            writeData(request.array())
            Timber.d("Unref resource: $resourceId")
        }
    }

    private fun writeData(data: ByteArray) {
        synchronized(mutex) {
            socket?.outputStream?.write(data)
                ?: throw IOException("Socket not connected")
        }
    }

    private fun readResponse(): ByteBuffer {
        synchronized(mutex) {
            val sock = socket ?: throw IOException("Socket not connected")
            val input = sock.inputStream

            // Read header (length + command)
            val header = ByteArray(8)
            var bytesRead = 0
            while (bytesRead < 8) {
                val n = input.read(header, bytesRead, 8 - bytesRead)
                if (n <= 0) throw IOException("Connection closed")
                bytesRead += n
            }

            val headerBuf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            val lengthDwords = headerBuf.getInt()
            val cmdId = headerBuf.getInt()

            // Read data if any
            val dataSize = lengthDwords * 4
            val fullResponse = ByteArray(8 + dataSize)
            System.arraycopy(header, 0, fullResponse, 0, 8)

            if (dataSize > 0) {
                bytesRead = 0
                while (bytesRead < dataSize) {
                    val n = input.read(fullResponse, 8 + bytesRead, dataSize - bytesRead)
                    if (n <= 0) throw IOException("Connection closed")
                    bytesRead += n
                }
            }

            return ByteBuffer.wrap(fullResponse).order(ByteOrder.LITTLE_ENDIAN)
        }
    }

    override fun close() {
        synchronized(mutex) {
            socket?.close()
            socket = null
            Timber.i("Disconnected from vtest server")
        }
    }
}
