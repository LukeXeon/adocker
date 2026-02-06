@file:Suppress("DEPRECATION")

package com.github.andock.daemon.os

import android.os.AsyncTask
import android.os.ParcelFileDescriptor
import timber.log.Timber
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.RejectedExecutionException

class RemoteProcessSession(
    cmd: Array<String>,
    env: Array<String>,
    dir: String?
) : IRemoteProcessSession.Stub() {

    companion object {
        private val executors = arrayOf(
            ForkJoinPool.commonPool(),
            AsyncTask.THREAD_POOL_EXECUTOR,
            Executors.newCachedThreadPool()
        )

        private fun execute(runnable: Runnable) {
            for (executor in executors) {
                try {
                    executor.execute(runnable)
                    return
                } catch (e: RejectedExecutionException) {
                    Timber.e(e)
                }
            }
        }

        private fun createPipeFd(stream: Any): ParcelFileDescriptor {
            val (read, write) = ParcelFileDescriptor.createReliablePipe()
            when (stream) {
                is InputStream -> {
                    execute {
                        try {
                            ParcelFileDescriptor.AutoCloseOutputStream(write).use { output ->
                                stream.use { input ->
                                    input.copyTo(output)
                                }
                            }
                        } catch (e: Exception) {
                            Timber.e(e)
                        }
                    }
                    return read
                }

                is OutputStream -> {
                    execute {
                        try {
                            stream.use { output ->
                                ParcelFileDescriptor.AutoCloseInputStream(read).use { input ->
                                    input.copyTo(output)
                                }
                            }
                        } catch (e: Exception) {
                            Timber.e(e)
                        }
                    }
                    return write
                }

                else -> {
                    throw AssertionError("Unknown stream type")
                }
            }
        }
    }

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
        createPipeFd(process.outputStream)
    }
    private val input by lazy {
        createPipeFd(process.inputStream)
    }
    private val error by lazy {
        createPipeFd(process.errorStream)
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

    override fun string(): String {
        return process.toString()
    }
}