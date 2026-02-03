@file:Suppress("PackageDirectoryMismatch")

package android.net

import android.net.LocalSocketAddress.Namespace
import android.system.Os
import java.io.File


fun LocalServerSocket(address: LocalSocketAddress): LocalServerSocket {
    val localSocket = LocalSocket()
    try {
        localSocket.bind(address)
        if (address.namespace == Namespace.FILESYSTEM) {
            Os.chmod(address.name, "660".toInt(8))
        }
    } catch (e: Exception) {
        localSocket.close()
        if (address.namespace == Namespace.FILESYSTEM) {
            File(address.name).delete()
        }
        throw e
    }
    return object : LocalServerSocket(localSocket.fileDescriptor) {
        override fun close() {
            try {
                localSocket.close()
            } finally {
                val address = localSocketAddress
                if (address.namespace == Namespace.FILESYSTEM) {
                    File(address.name).delete()
                }
            }
        }
    }
}
