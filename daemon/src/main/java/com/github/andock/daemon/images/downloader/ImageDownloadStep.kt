package com.github.andock.daemon.images.downloader

import kotlinx.coroutines.flow.StateFlow

class ImageDownloadStep(
    val name: String,
    val progress: StateFlow<Float>
)