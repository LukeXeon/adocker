package com.github.andock.ui.utils

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.uuid.ExperimentalUuidApi

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


typealias SavedStateHandleKey<T> = Pair<String, T>

@OptIn(ExperimentalUuidApi::class)
fun <T> savedStateHandleKey(initialValue: T): ReadOnlyProperty<Any?, SavedStateHandleKey<T>> {
    return object : ReadOnlyProperty<Any?, SavedStateHandleKey<T>> {
        @Volatile
        private var value: SavedStateHandleKey<T>? = null

        override fun getValue(thisRef: Any?, property: KProperty<*>): SavedStateHandleKey<T> {
            val v = value
            if (v == null) {
                synchronized(this) {
                    var v2 = value
                    if (v2 == null) {
                        v2 = property.name to initialValue
                        value = v2
                    }
                    return v2
                }
            } else {
                return v
            }
        }
    }
}

operator fun <T> SavedStateHandle.get(key: SavedStateHandleKey<T>): MutableStateFlow<T> {
    return getMutableStateFlow(key.first, key.second)
}
