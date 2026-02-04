package com.github.andock.common

import android.os.SystemClock
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

data class TimeMillisWithResult<T>(
    val result: T,
    val timeMillis: Long,
)

@OptIn(ExperimentalContracts::class)
inline fun measureTimeMillis(block: () -> Unit): Long {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val start = SystemClock.uptimeMillis()
    block()
    return SystemClock.uptimeMillis() - start
}

@OptIn(ExperimentalContracts::class)
inline fun <T> measureTimeMillisWithResult(block: () -> T): TimeMillisWithResult<T> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val start = SystemClock.uptimeMillis()
    return TimeMillisWithResult(block(), SystemClock.uptimeMillis() - start)
}