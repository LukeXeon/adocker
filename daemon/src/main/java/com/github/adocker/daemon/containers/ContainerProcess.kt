package com.github.adocker.daemon.containers

import android.os.Handler
import android.os.Looper
import android.os.MessageQueue.OnFileDescriptorEventListener
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.FilterOutputStream
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Singleton
import kotlin.coroutines.resume

class ContainerProcess @AssistedInject constructor(
    @Assisted
    private val process: Process,
    scope: CoroutineScope
) {

    companion object {
        private val getFileDescriptor by lazy<Result<(Any) -> FileDescriptor>> {
            runCatching {
                FilterOutputStream::class.java.getDeclaredField("out").apply {
                    isAccessible = true
                }
            }.map { field ->
                return@map { stream ->
                    field.get(stream).let {
                        it as FileOutputStream
                    }.fd
                }
            }
        }
        private val queue by lazy {
            Class.forName("android.app.QueuedWork")
                .getDeclaredMethod(
                    "getHandler"
                ).apply {
                    isAccessible = true
                }.runCatching { invoke(null) as Handler }
                .onFailure { e ->
                    Timber.d(e)
                }.map { it.looper }
                .getOrElse { Looper.getMainLooper() }.queue
        }
    }

    val job = scope.launch {
        try {
            suspendCancellableCoroutine { con ->
                getFileDescriptor.mapCatching {
                    it(process.outputStream)
                }.fold(
                    { fd ->
                        val callbacks = object : OnFileDescriptorEventListener, CompletionHandler {
                            override fun onFileDescriptorEvents(
                                fd: FileDescriptor,
                                events: Int
                            ): Int {
                                con.resume(Unit)
                                return 0
                            }

                            override fun invoke(p1: Throwable?) {
                                queue.removeOnFileDescriptorEventListener(fd)
                            }
                        }
                        queue.addOnFileDescriptorEventListener(
                            fd,
                            OnFileDescriptorEventListener.EVENT_ERROR,
                            callbacks
                        )
                        con.invokeOnCancellation(callbacks)
                    },
                    {
                        Timber.d(it)
                        con.resume(Unit)
                    }
                )
            }
            if (process.isAlive) {
                withContext(Dispatchers.IO) {
                    runInterruptible {
                        process.waitFor()
                    }
                }
            }
        } catch (e: CancellationException) {
            process.destroy()
            process.waitFor()
            throw e
        }
    }

    val exitCode: Int
        get() = process.exitValue()

    val stdin: OutputStream
        get() = process.outputStream

    val stdout: InputStream
        get() = process.inputStream

    val stderr: InputStream
        get() = process.errorStream

    @Singleton
    @AssistedFactory
    interface Factory {
        fun create(@Assisted process: Process): ContainerProcess
    }
}