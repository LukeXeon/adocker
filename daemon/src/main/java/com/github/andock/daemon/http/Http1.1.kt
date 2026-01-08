package com.github.andock.daemon.http

import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.MemoryBody
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream



private const val HTTP_VERSION = "HTTP/1.1"
private const val CRLF = "\r\n"

/**
 * Parses an HTTP/1.1 request from the input stream.
 * Returns null if the connection was closed or the request is invalid.
 */
private fun readRequest(inputStream: InputStream): Request? {
    val reader = inputStream.bufferedReader()
    // Read request line
    val requestLine = reader.readLine() ?: return null
    val requestParts = requestLine.split(" ")
    if (requestParts.size < 3) return null

    val method = try {
        Method.valueOf(requestParts[0])
    } catch (_: IllegalArgumentException) {
        return null
    }
    val uri = requestParts[1]

    // Read headers
    val headers = mutableListOf<Pair<String, String?>>()
    var contentLength = 0L

    while (true) {
        val line = reader.readLine() ?: break
        if (line.isEmpty()) break

        val colonIndex = line.indexOf(':')
        if (colonIndex > 0) {
            val name = line.substring(0, colonIndex).trim()
            val value = line.substring(colonIndex + 1).trim()
            headers.add(name to value)

            if (name.equals("Content-Length", ignoreCase = true)) {
                contentLength = value.toLongOrNull() ?: 0L
            }
        }
    }

    // Read body if present
    val body: Body = if (contentLength > 0) {
        val bodyBytes = ByteArray(contentLength.toInt())
        var bytesRead = 0
        while (bytesRead < contentLength) {
            val read = inputStream.read(
                bodyBytes,
                bytesRead,
                (contentLength - bytesRead).toInt()
            )
            if (read == -1) break
            bytesRead += read
        }
        MemoryBody(bodyBytes)
    } else {
        Body.EMPTY
    }

    return Request(method, uri)
        .headers(headers)
        .body(body)
}

/**
 * Writes an HTTP/1.1 response to the output stream.
 */
private fun writeResponse(response: Response, outputStream: OutputStream) {
    val statusLine =
        "$HTTP_VERSION ${response.status.code} ${response.status.description}$CRLF"
    outputStream.write(statusLine.toByteArray(Charsets.UTF_8))

    // Write headers
    val bodyBytes = response.body.stream.readBytes()
    val headersWithContentLength = if (response.header("Content-Length") == null) {
        response.headers + ("Content-Length" to bodyBytes.size.toString())
    } else {
        response.headers
    }

    for ((name, value) in headersWithContentLength) {
        if (value != null) {
            outputStream.write("$name: $value$CRLF".toByteArray(Charsets.UTF_8))
        }
    }

    // End of headers
    outputStream.write(CRLF.toByteArray(Charsets.UTF_8))

    // Write body
    if (bodyBytes.isNotEmpty()) {
        outputStream.write(bodyBytes)
    }

    outputStream.flush()
}

/**
 * Handles a single client connection.
 * Supports HTTP/1.1 keep-alive connections.
 */
fun HttpHandler.process(connection: ClientConnection) {
    connection.use { conn ->
        try {
            // For simplicity, handle one request per connection
            // Keep-alive can be added later if needed
            val request = readRequest(conn.inputStream)

            if (request != null) {
                Timber.d("Received request: ${request.method} ${request.uri}")
                val response = try {
                    invoke(request)
                } catch (e: Exception) {
                    Timber.e(e, "Error handling request")
                    Response(Status.INTERNAL_SERVER_ERROR)
                        .body("Internal Server Error: ${e.message}")
                }
                writeResponse(response, conn.outputStream)
            } else {
                Timber.w("Failed to parse request")
                val errorResponse = Response(Status.BAD_REQUEST)
                    .body("Bad Request")
                writeResponse(errorResponse, conn.outputStream)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in connection handler")
        }
    }
}