package com.github.andock.ui.utils

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue


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
