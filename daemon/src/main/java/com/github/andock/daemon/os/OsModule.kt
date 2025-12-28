package com.github.andock.daemon.os

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FilterInputStream
import java.io.FilterOutputStream
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OsModule {
    @SuppressLint("DiscouragedPrivateApi", "PrivateApi")
    @Provides
    @Singleton
    fun awaiter(): ProcessAwaiter {
        runCatching {
            arrayOf(
                FilterOutputStream::class.java to "out",
                FilterInputStream::class.java to "in",
            ).map {
                it.first.getDeclaredField(it.second).apply {
                    isAccessible = true
                }
            }
        }.fold(
            { fields ->
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
                        when (stream) {
                            is FilterOutputStream -> {
                                return fields[0].runCatching {
                                    get(stream)
                                }.mapCatching {
                                    it as FileOutputStream
                                }.mapCatching {
                                    it.fd
                                }
                            }

                            is FilterInputStream -> {
                                return fields[1].runCatching {
                                    get(stream)
                                }.mapCatching {
                                    it as FileInputStream
                                }.mapCatching {
                                    it.fd
                                }
                            }

                            else -> {
                                throw IllegalArgumentException()
                            }
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