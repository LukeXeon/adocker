package com.github.adocker.daemon.aio.backend

import android.net.LocalSocket
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.SocketAddress
import io.ktor.network.sockets.UnixSocketAddress
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ReaderJob
import io.ktor.utils.io.WriterJob
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

internal class UnixSocketImpl(
    private val socket: LocalSocket,
) : Socket {

    override fun close() {
        TODO("Not yet implemented")
    }

    override fun attachForReading(channel: ByteChannel): WriterJob {
        TODO("Not yet implemented")
    }

    override fun attachForWriting(channel: ByteChannel): ReaderJob {
        TODO("Not yet implemented")
    }

    override val coroutineContext: CoroutineContext
        get() = EmptyCoroutineContext
    override val socketContext: Job
        get() = TODO("Not yet implemented")
    override val localAddress: SocketAddress
        get() = UnixSocketAddress(socket.localSocketAddress.name)

    override val remoteAddress: SocketAddress
        get() = UnixSocketAddress("")
}