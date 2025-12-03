package com.github.adocker.daemon.aio.backend

import android.net.LocalServerSocket
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.SocketAddress
import io.ktor.network.sockets.UnixSocketAddress
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.IOException

internal class UnixServerSocketImpl(
    path: String,
) : ServerSocket {

    override val socketContext: CompletableJob = Job()

    private val serverSocket = LocalServerSocket(path)

    private val channel = Channel<Socket>()

    init {
        CoroutineScope(socketContext + Dispatchers.IO).launch {
            while (true) {
                val socket = try {
                    serverSocket.accept()
                } catch (e: IOException) {
                    if (e.message == "socket not created") {
                        break
                    } else {
                        continue
                    }
                }
                channel.send(UnixSocketImpl(socket))
            }
        }
    }


    override fun close() {
        try {
            serverSocket.close()
            socketContext.complete()
        } catch (cause: Throwable) {
            socketContext.completeExceptionally(cause)
        }
    }

    override suspend fun accept(): Socket {
        return channel.receive()
    }

    override val localAddress: SocketAddress = UnixSocketAddress(path)
}