package com.github.adocker.daemon.os

import android.os.ParcelFileDescriptor
import android.os.RemoteException
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream

class RemoteProcess(
    private val session: IRemoteProcessSession
) : Process() {
    companion object {
        private val processes = HashSet<Process>()
    }

    init {
        try {
            session.asBinder().linkToDeath({
                synchronized(Companion) {
                    processes.remove(this)
                }
            }, 0)
            // The reference to the binder object must be hold
            synchronized(Companion) {
                processes.add(this)
            }
        } catch (e: RemoteException) {
            Timber.e(e, "linkToDeath")
        }
    }

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
        try {
            return session.exitValue()
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        }
    }

    override fun destroy() {
        try {
            return session.destroy()
        } catch (e: RemoteException) {
            throw RuntimeException(e)
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
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        }
    }
}