package com.github.adocker.daemon.http

import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

/**
 * Client connection implementation for TCP sockets.
 */
class TcpClientConnection(
    private val socket: Socket
) : ClientConnection {

    override val inputStream: InputStream
        get() = socket.inputStream

    override val outputStream: OutputStream
        get() = socket.outputStream

    override fun close() {
        socket.close()
    }
}
