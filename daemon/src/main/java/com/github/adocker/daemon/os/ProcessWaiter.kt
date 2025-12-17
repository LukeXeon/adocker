package com.github.adocker.daemon.os

import android.os.MessageQueue
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.FileDescriptor
import kotlin.coroutines.resume

sealed interface ProcessWaiter {
    suspend fun waitFor(process: Process): Int

    object Blocking : ProcessWaiter {
        override suspend fun waitFor(process: Process): Int {
            return withContext(Dispatchers.IO) {
                runInterruptible {
                    process.waitFor()
                }
            }
        }
    }

    class NonBlocking(
        private val getFileDescriptor: (Any) -> FileDescriptor,
        private val queue: MessageQueue
    ) : ProcessWaiter {
        override suspend fun waitFor(process: Process): Int {
            suspendCancellableCoroutine { con ->
                getFileDescriptor.runCatching {
                    invoke(process.outputStream)
                }.fold(
                    { fd ->
                        val callbacks = object : MessageQueue.OnFileDescriptorEventListener,
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
                            MessageQueue.OnFileDescriptorEventListener.EVENT_ERROR,
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
            return if (process.isAlive) {
                Blocking.waitFor(process)
            } else {
                process.exitValue()
            }
        }
    }
}