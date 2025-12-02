/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package com.github.adocker.daemon.aio

import io.ktor.http.*
import io.ktor.http.cio.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.request.*
import io.ktor.util.network.*
import io.ktor.utils.io.*

internal class AIOApplicationRequest(
    call: PipelineCall,
    remoteAddress: NetworkAddress?,
    localAddress: NetworkAddress?,
    input: ByteReadChannel,
    private val request: Request
) : BaseApplicationRequest(call) {
    override val cookies: RequestCookies by lazy { RequestCookies(this) }

    override val engineReceiveChannel: ByteReadChannel = input

    override var engineHeaders: Headers = CIOHeaders(request.headers)

    @OptIn(InternalAPI::class)
    override val queryParameters: Parameters by lazy {
        encodeParameters(rawQueryParameters).withEmptyStringForValuelessKeys()
    }

    override val rawQueryParameters: Parameters by lazy {
        val uri = request.uri.toString()
        val queryStartIndex = uri.indexOf('?').takeIf { it != -1 } ?: return@lazy Parameters.Empty
        parseQueryString(uri, startIndex = queryStartIndex + 1, decode = false)
    }

    override val local: RequestConnectionPoint = AIOConnectionPoint(
        remoteAddress,
        localAddress,
        request.version.toString(),
        request.uri.toString(),
        request.headers[HttpHeaders.Host]?.toString(),
        HttpMethod.parse(request.method.value)
    )

    internal fun release() {
        request.release()
    }
}

