package com.github.andock.startup

import android.os.SystemClock
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

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