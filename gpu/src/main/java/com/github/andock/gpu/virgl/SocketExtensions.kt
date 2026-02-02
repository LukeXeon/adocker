package com.github.andock.gpu.virgl

import android.net.LocalSocket
import java.io.FileDescriptor

/**
 * Extensions for LocalSocket to support file descriptor passing
 */

/**
 * Send a file descriptor through the socket using ancillary data (SCM_RIGHTS)
 *
 * @param fd File descriptor to send
 * @throws IOException if send fails
 */
fun LocalSocket.sendFileDescriptor(fd: FileDescriptor) {
    // Android LocalSocket supports fd passing via setFileDescriptorsForSend
    val method = LocalSocket::class.java.getDeclaredMethod(
        "setFileDescriptorsForSend",
        Array<FileDescriptor>::class.java
    )
    method.isAccessible = true
    method.invoke(this, arrayOf(fd))

    // Send a dummy byte to trigger the fd transfer
    outputStream.write(byteArrayOf(0x42))
    outputStream.flush()
}

/**
 * Receive a file descriptor from the socket using ancillary data (SCM_RIGHTS)
 *
 * @return File descriptor received
 * @throws IOException if receive fails
 */
fun LocalSocket.receiveFileDescriptor(): FileDescriptor? {
    // Read the dummy byte first
    val dummy = ByteArray(1)
    val read = inputStream.read(dummy)
    if (read != 1) {
        throw java.io.IOException("Failed to receive fd: no data")
    }

    // Get the received file descriptors
    val method = LocalSocket::class.java.getDeclaredMethod("getAncillaryFileDescriptors")
    method.isAccessible = true
    val fds = method.invoke(this) as? Array<*>

    return fds?.firstOrNull() as? FileDescriptor
}
