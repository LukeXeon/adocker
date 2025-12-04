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
    dir: String?
) : IRemoteProcessSession.Stub() {
    private val process = Runtime.getRuntime().exec(
        cmd,
        env,
        if (dir == null) {
            null
        } else {
            File(dir)
        }
    )
    private val output by lazy {
        val (read, write) = ParcelFileDescriptor.createReliablePipe()
        GlobalScope.launch(Dispatchers.IO) {
            process.outputStream.use { output ->
                ParcelFileDescriptor.AutoCloseInputStream(read).use { input ->
                    input.copyTo(output)
                }
            }
        }
        return@lazy write
    }
    private val input by lazy {
        val (read, write) = ParcelFileDescriptor.createReliablePipe()
        GlobalScope.launch(Dispatchers.IO) {
            ParcelFileDescriptor.AutoCloseOutputStream(write).use { output ->
                process.inputStream.use { input ->
                    input.copyTo(output)
                }
            }
        }
        return@lazy read
    }
    private val error by lazy {
        val (read, write) = ParcelFileDescriptor.createReliablePipe()
        GlobalScope.launch(Dispatchers.IO) {
            ParcelFileDescriptor.AutoCloseOutputStream(write).use { output ->
                process.errorStream.use { input ->
                    input.copyTo(output)
                }
            }
        }
        return@lazy read
    }

    override fun getOutputStream(): ParcelFileDescriptor {
        return output
    }

    override fun getInputStream(): ParcelFileDescriptor {
        return input
    }

    override fun getErrorStream(): ParcelFileDescriptor {
        return error
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