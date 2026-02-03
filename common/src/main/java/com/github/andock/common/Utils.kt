package com.github.andock.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


suspend inline fun <T> withAtLeast(
    timeMillis: Long,
    crossinline block: suspend CoroutineScope.() -> T
): T {
    return coroutineScope {
        val job = launch {
            delay(timeMillis)
        }
        val value = block()
        job.join()
        value
    }
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