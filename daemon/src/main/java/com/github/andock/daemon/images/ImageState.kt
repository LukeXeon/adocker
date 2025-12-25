package com.github.andock.daemon.images

import kotlinx.coroutines.flow.StateFlow

sealed interface ImageState {
    val id: String

    data class Waiting(
        override val id: String
    ) : ImageState

    data class Downloading(
        override val id: String,
        val progress: StateFlow<List<DownloadProgress>>
    ) : ImageState

    data class Downloaded(override val id: String) : ImageState

    data class Removing(override val id: String) : ImageState

    data class Removed(override val id: String) : ImageState

    data class Error(
        override val id: String,
        val throwable: Throwable,
    ) : ImageState
}