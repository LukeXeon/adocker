package com.github.adocker.daemon.aio.backend

import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.SocketAddress
import io.ktor.network.sockets.toJavaAddress
import io.ktor.util.network.NetworkAddress

internal val SocketAddress.port: Int?
    get() = (this as? InetSocketAddress)?.port

internal fun SocketAddress.toNetworkAddress(): NetworkAddress? {
    // Do not read the hostname here because that may trigger a name service reverse lookup.
    return toJavaAddress() as? java.net.InetSocketAddress
}