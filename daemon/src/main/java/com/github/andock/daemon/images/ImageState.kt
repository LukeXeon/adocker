package com.github.andock.daemon.images

import kotlinx.coroutines.flow.StateFlow

sealed interface ImageState {
    val digest: String

    data class Downloading(
        override val digest: String,
        val progress: StateFlow<List<DownloadProgress>>
    ) : ImageState

    data class Downloaded(override val digest: String) : ImageState

    data class Removing(override val digest: String) : ImageState

    data class Removed(override val digest: String) : ImageState

    data class Error(
        override val digest: String,
        val throwable: Throwable,
    ) : ImageState
}