package com.github.andock.daemon.http

import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream

/**
 * Represents a client connection abstraction that works with both
 * TCP sockets and Unix domain sockets.
 */
interface ClientConnection : Closeable {
    val inputStream: InputStream
    val outputStream: OutputStream
}
