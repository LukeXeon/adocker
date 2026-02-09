package com.github.andock.common

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import kotlinx.coroutines.delay
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

suspend inline fun <T> withAtLeast(
    timeMillis: Long,
    block: suspend () -> T
): T {
    val start = SystemClock.uptimeMillis()
    val value = block()
    val elapsed = SystemClock.uptimeMillis() - start
    if (elapsed < timeMillis) {
        delay(timeMillis - elapsed)
    }
    return value
}

fun formatDate(timestamp: Long): String {
    return SimpleDateFormat(
        "yyyy-MM-dd HH:mm:ss",
        Locale.getDefault()
    ).format(Date(timestamp))
}

fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

val queuedWorkLooper: Looper by lazy {
    runCatching {
        Class.forName("android.os.QueuedWork")
            .getDeclaredMethod("getHandler").apply {
                isAccessible = true
            }(null) as Handler
    }.map {
        it.looper
    }.recover {
        Timber.e(it, "access android.os.QueuedWork failed")
        HandlerThread(
            "queued-work-looper",
            Process.THREAD_PRIORITY_FOREGROUND
        ).apply {
            start()
        }.looper
    }.getOrThrow()
}