package com.github.adocker.daemon.os

import android.os.Handler
import android.os.Looper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import java.io.FileOutputStream
import java.io.FilterOutputStream
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OsModule {
    @Provides
    @Singleton
    fun waiter(): ProcessWaiter {
        runCatching {
            FilterOutputStream::class.java.getDeclaredField("out").apply {
                isAccessible = true
            }
        }.map { field ->
            return@map { stream: Any ->
                field.runCatching {
                    get(stream)
                }.map {
                    it as FileOutputStream
                }.map {
                    it.fd
                }
            }
        }.fold(
            {
                val queue = Class.forName("android.app.QueuedWork")
                    .getDeclaredMethod(
                        "getHandler"
                    ).apply {
                        isAccessible = true
                    }.runCatching { invoke(null) as Handler }
                    .onFailure { e ->
                        Timber.d(e)
                    }.map { h -> h.looper }
                    .getOrElse { Looper.getMainLooper() }.queue
                return ProcessWaiter.NonBlocking(it, queue)
            },
            {
                return ProcessWaiter.Blocking
            }
        )
    }
}