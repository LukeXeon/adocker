package com.github.andock.daemon.images

import com.github.andock.daemon.client.DownloadProgress
import com.github.andock.daemon.client.ImageReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface ImageDownloadState {

    val imageRef: ImageReference

    class Downloading(
        override val imageRef: ImageReference
    ) : ImageDownloadState {
        private val mutex = Mutex()
        private val _progress = MutableStateFlow(emptyMap<String, DownloadProgress>())

        val progress = _progress.asStateFlow()

        internal suspend inline fun updateProgress(function: (Map<String, DownloadProgress>) -> Map<String, DownloadProgress>) {
            mutex.withLock {
                _progress.update(function)
            }
        }

    }

    data class Error(
        override val imageRef: ImageReference,
        val throwable: Throwable
    ) : ImageDownloadState

    data class Done(
        override val imageRef: ImageReference
    ) : ImageDownloadState
}