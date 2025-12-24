package com.github.andock.daemon.os

import android.os.Handler
import android.os.Looper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.FilterOutputStream
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OsModule {
    @Provides
    @Singleton
    fun awaiter(): ProcessAwaiter {
        runCatching {
            FilterOutputStream::class.java.getDeclaredField("out").apply {
                isAccessible = true
            }
        }.fold(
            { field ->
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
                return object : ProcessAwaiter.NonBlocking(queue) {
                    override fun getFileDescriptor(stream: Any): Result<FileDescriptor> {
                        return field.runCatching {
                            get(stream)
                        }.mapCatching {
                            it as FileOutputStream
                        }.mapCatching {
                            it.fd
                        }
                    }
                }
            },
            {
                return ProcessAwaiter.Blocking
            }
        )
    }

    @Provides
    @Singleton
    fun locator(): ProcessLocator {
        Runtime.getRuntime().runCatching {
            exec("echo").apply {
                destroy()
            }.javaClass.getDeclaredField("pid").apply {
                isAccessible = true
            }
        }.onFailure { e ->
            Timber.d(e)
        }.fold(
            { field ->
                return object : ProcessLocator.Reflection() {
                    override fun getField(process: Process): Int {
                        return field.runCatching {
                            getInt(process)
                        }.getOrDefault(0)
                    }
                }
            },
            {
                return ProcessLocator.Parser
            }
        )
    }
}