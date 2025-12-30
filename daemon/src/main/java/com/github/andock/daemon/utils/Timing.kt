package com.github.andock.daemon.utils

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