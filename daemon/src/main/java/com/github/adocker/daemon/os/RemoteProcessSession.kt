package com.github.adocker.daemon.os

import android.os.ParcelFileDescriptor
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

@OptIn(DelicateCoroutinesApi::class)
class RemoteProcessSession(
    cmd: Array<String>,
    env: Array<String>,
    dir: String
) : IRemoteProcessSession.Stub() {
    private val process = Runtime.getRuntime().exec(cmd, env, File(dir))

    override fun getOutputStream(): ParcelFileDescriptor {
        val (read, write) = ParcelFileDescriptor.createReliablePipe()
        GlobalScope.launch(Dispatchers.IO) {
            ParcelFileDescriptor.AutoCloseInputStream(read).use { input ->
                input.copyTo(process.outputStream)
            }
        }
        return write
    }

    override fun getInputStream(): ParcelFileDescriptor {
        val (read, write) = ParcelFileDescriptor.createReliablePipe()
        GlobalScope.launch(Dispatchers.IO) {
            ParcelFileDescriptor.AutoCloseOutputStream(write).use { output ->
                process.inputStream.copyTo(output)
            }
        }
        return read
    }

    override fun getErrorStream(): ParcelFileDescriptor {
        val (read, write) = ParcelFileDescriptor.createReliablePipe()
        GlobalScope.launch(Dispatchers.IO) {
            ParcelFileDescriptor.AutoCloseOutputStream(write).use { output ->
                process.errorStream.copyTo(output)
            }
        }
        return read
    }

    override fun waitFor(): Int {
        return process.waitFor()
    }

    override fun exitValue(): Int {
        return process.exitValue()
    }

    override fun destroy() {
        process.destroy()
    }

    override fun isAlive(): Boolean {
        return process.isAlive
    }
}