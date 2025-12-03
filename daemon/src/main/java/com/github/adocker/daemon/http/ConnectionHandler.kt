package com.github.adocker.daemon.http

import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status
import timber.log.Timber

/**
 * Handles HTTP connections by parsing requests and writing responses.
 * Shared logic between TCP and Unix socket server implementations.
 */
class ConnectionHandler(
    private val httpHandler: HttpHandler
) {

    /**
     * Handles a single client connection.
     * Supports HTTP/1.1 keep-alive connections.
     */
    fun handle(connection: ClientConnection) {
        connection.use { conn ->
            try {
                // For simplicity, handle one request per connection
                // Keep-alive can be added later if needed
                val request = HttpParser.parseRequest(conn.inputStream)

                if (request != null) {
                    Timber.d("Received request: ${request.method} ${request.uri}")
                    val response = try {
                        httpHandler(request)
                    } catch (e: Exception) {
                        Timber.e(e, "Error handling request")
                        Response(Status.INTERNAL_SERVER_ERROR)
                            .body("Internal Server Error: ${e.message}")
                    }
                    HttpParser.writeResponse(response, conn.outputStream)
                } else {
                    Timber.w("Failed to parse request")
                    val errorResponse = Response(Status.BAD_REQUEST)
                        .body("Bad Request")
                    HttpParser.writeResponse(errorResponse, conn.outputStream)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error in connection handler")
            }
        }
    }
}
