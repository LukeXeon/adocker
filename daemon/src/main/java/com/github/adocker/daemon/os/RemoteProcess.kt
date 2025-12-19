package com.github.adocker.daemon.os

import android.os.DeadObjectException
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream

class RemoteProcess(
    private val session: IRemoteProcessSession
) : Process() {
    private val output by lazy {
        ParcelFileDescriptor.AutoCloseOutputStream(session.outputStream)
    }
    private val input by lazy {
        ParcelFileDescriptor.AutoCloseInputStream(session.inputStream)
    }
    private val error by lazy {
        ParcelFileDescriptor.AutoCloseInputStream(session.errorStream)
    }

    override fun getOutputStream(): OutputStream {
        return output
    }

    override fun getInputStream(): InputStream {
        return input
    }

    override fun getErrorStream(): InputStream {
        return error
    }

    override fun exitValue(): Int {
        return try {
            session.exitValue()
        } catch (e: DeadObjectException) {
            Timber.e(e)
            -1
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        }
    }

    override fun destroy() {
        try {
            return session.destroy()
        } catch (e: RemoteException) {
            Timber.e(e)
        }
    }

    override fun waitFor(): Int {
        try {
            return session.waitFor()
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        }
    }

    override fun isAlive(): Boolean {
        try {
            return session.isAlive
        } catch (e: Exception) {
            Timber.e(e)
            return false
        }
    }

    override fun toString(): String {
        try {
            return session.toString()
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        }
    }
}