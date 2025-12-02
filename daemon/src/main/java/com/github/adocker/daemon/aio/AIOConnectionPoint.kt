package com.github.adocker.daemon.aio

import io.ktor.http.HttpMethod
import io.ktor.http.RequestConnectionPoint
import io.ktor.http.URLProtocol
import io.ktor.util.network.NetworkAddress
import io.ktor.util.network.address
import io.ktor.util.network.hostname
import io.ktor.util.network.port

internal class AIOConnectionPoint(
    private val remoteNetworkAddress: NetworkAddress?,
    private val localNetworkAddress: NetworkAddress?,
    override val version: String,
    override val uri: String,
    private val hostHeaderValue: String?,
    override val method: HttpMethod
) : RequestConnectionPoint {
    override val scheme: String
        get() = "http"

    private val defaultPort = URLProtocol.Companion.createOrDefault(scheme).defaultPort

    @Deprecated("Use localPort or serverPort instead")
    override val host: String
        get() = localNetworkAddress?.hostname
            ?: hostHeaderValue?.substringBefore(":")
            ?: "localhost"

    @Deprecated("Use localPort or serverPort instead")
    override val port: Int
        get() = localNetworkAddress?.port
            ?: hostHeaderValue?.substringAfter(":", "80")?.toInt()
            ?: 80

    override val localPort: Int
        get() = localNetworkAddress?.port ?: defaultPort

    override val serverPort: Int
        get() = hostHeaderValue
            ?.substringAfterLast(":", defaultPort.toString())?.toInt()
            ?: localPort

    override val localHost: String
        get() = localNetworkAddress?.hostname ?: "localhost"

    override val serverHost: String
        get() = hostHeaderValue?.substringBeforeLast(":") ?: localHost

    override val localAddress: String
        get() = localNetworkAddress?.address ?: "localhost"

    override val remoteHost: String
        get() = remoteNetworkAddress?.hostname ?: "unknown"

    override val remotePort: Int
        get() = remoteNetworkAddress?.port ?: 0

    override val remoteAddress: String
        get() = remoteNetworkAddress?.address ?: "unknown"

    override fun toString(): String =
        "CIOConnectionPoint(uri=$uri, method=$method, version=$version, localAddress=$localAddress, " +
            "localPort=$localPort, remoteAddress=$remoteAddress, remotePort=$remotePort)"
}