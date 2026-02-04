package com.github.andock.common

import android.app.ActivityThread
import android.app.Application
import android.os.SystemClock
import android.view.inspector.WindowInspector
import kotlinx.coroutines.delay
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

val application by lazy(LazyThreadSafetyMode.PUBLICATION) {
    WindowInspector.getGlobalWindowViews().firstOrNull()
        ?.context
        ?.applicationContext as? Application
        ?: ActivityThread.currentApplication()
}