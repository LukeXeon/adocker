package com.github.adocker.ui.screens.home

// Get stats
data class HomeStats(
    val totalImages: Int,
    val totalContainers: Int,
    val runningContainers: Int,
    val stoppedContainers: Int
)