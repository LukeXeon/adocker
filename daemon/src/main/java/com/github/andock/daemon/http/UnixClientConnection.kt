package com.github.andock.daemon.http

import android.net.LocalSocket
import java.io.InputStream
import java.io.OutputStream

/**
 * Client connection implementation for Unix domain sockets.
 */
class UnixClientConnection(
    private val socket: LocalSocket
) : ClientConnection {

    override val inputStream: InputStream
        get() = socket.inputStream

    override val outputStream: OutputStream
        get() = socket.outputStream

    override fun close() {
        socket.close()
    }
}
