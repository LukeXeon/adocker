package com.github.adocker.ui.screens.home

// Get stats
data class HomeStats(
    val totalImages: Int = 0,
    val totalContainers: Int = 0,
    val runningContainers: Int = 0,
    val stoppedContainers: Int = 0
)