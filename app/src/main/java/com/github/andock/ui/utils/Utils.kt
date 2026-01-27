package com.github.andock.ui.utils

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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


@Composable
fun debounceClick(debounceTime: Long = 500, onClick: () -> Unit): () -> Unit {
    var lastClickTime by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    val onClick by rememberUpdatedState(onClick)
    val debounceTime by rememberUpdatedState(debounceTime)
    return remember {
        {
            val currentTime = SystemClock.uptimeMillis()
            if (currentTime - lastClickTime >= debounceTime) {
                lastClickTime = currentTime
                onClick()
            }
        }
    }
}
