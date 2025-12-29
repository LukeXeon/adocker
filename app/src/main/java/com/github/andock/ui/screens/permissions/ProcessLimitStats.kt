package com.github.andock.ui.screens.permissions

data class ProcessLimitStats(
    val isAvailable: Boolean = false,
    val hasPermission: Boolean = false,
    val isUnrestricted: Boolean = false,
    val currentLimit: Int = 32,
)