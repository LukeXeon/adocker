package com.github.andock.ui.screens.limits

data class ProcessLimitStats(
    val isInstalled: Boolean = false,
    val isAvailable: Boolean = false,
    val isUnrestricted: Boolean = false,
    val currentLimit: Int = 32,
)