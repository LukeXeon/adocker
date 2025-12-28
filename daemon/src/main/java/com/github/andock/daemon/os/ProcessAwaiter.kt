package com.github.andock.daemon.os

import android.os.MessageQueue
import android.os.MessageQueue.OnFileDescriptorEventListener
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.FileDescriptor
import kotlin.coroutines.resume

sealed interface ProcessAwaiter {
    suspend fun await(process: Process): Int

    object Blocking : ProcessAwaiter {
        override suspend fun await(process: Process): Int {
            return withContext(Dispatchers.IO) {
                runInterruptible {
                    process.waitFor()
                }
            }
        }
    }

    abstract class NonBlocking(
        private val queue: MessageQueue
    ) : ProcessAwaiter {
        override suspend fun await(process: Process): Int {
            suspendCancellableCoroutine { con ->
                val fd = sequenceOf(
                    process.inputStream,
                    process.outputStream,
                    process.errorStream
                ).mapNotNull {
                    getFileDescriptor(it).getOrNull()
                }.filter {
                    it.valid()
                }.firstOrNull()
                if (fd != null) {
                    val callbacks = object : OnFileDescriptorEventListener,
                        CompletionHandler {
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
                    if (!process.isAlive) {
                        con.resume(Unit)
                    }
                } else {
                    con.resume(Unit)
                }
            }
            return if (process.isAlive) {
                Blocking.await(process)
            } else {
                process.exitValue()
            }
        }

        abstract fun getFileDescriptor(stream: Any): Result<FileDescriptor>
    }
}