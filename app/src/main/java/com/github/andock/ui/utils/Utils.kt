package com.github.andock.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun formatDate(timestamp: Long): String {
    val sdf = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }
    return sdf.format(Date(timestamp))
}

fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

suspend inline fun withAtLeast(
    timeMillis: Long,
    crossinline block: suspend CoroutineScope.() -> Unit
) {
    coroutineScope {
        val job = launch {
            delay(timeMillis)
        }
        block()
        job.join()
    }
}