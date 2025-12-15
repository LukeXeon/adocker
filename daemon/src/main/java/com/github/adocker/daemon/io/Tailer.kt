package com.github.adocker.daemon.io

import com.github.adocker.daemon.app.AppGlobals
import kotlinx.coroutines.launch
import org.apache.commons.io.input.Tailer
import org.apache.commons.io.input.TailerListenerAdapter
import java.io.File
import java.time.Duration
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit

fun tailerFlow(file: File): Tailer {
    val scope = AppGlobals.scope()
    return Tailer.builder()
        .setFile(file)
        .setExecutorService(object : AbstractExecutorService() {
            override fun execute(command: Runnable) {
                scope.launch {
                    command.run()
                }
            }

            override fun shutdown() {
                throw UnsupportedOperationException("shutdown is not implemented")
            }

            override fun shutdownNow(): MutableList<Runnable> {
                throw UnsupportedOperationException("shutdownNow is not implemented")
            }

            override fun isShutdown(): Boolean = false

            override fun isTerminated(): Boolean = false

            override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean = false
        })
        .setTailerListener(object : TailerListenerAdapter() {

        })
        .setDelayDuration(Duration.ofMillis(100))
        .setStartThread(true)
        .setTailFromEnd(false) // start from beginning
        .setReOpen(false) // don't reopen on EOF
        .get()
}