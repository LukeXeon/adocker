package com.github.andock.daemon.images.downloader

import com.github.andock.daemon.images.DownloadProgress
import com.github.andock.daemon.images.ImageReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface ImageDownloadState {

    val ref: ImageReference

    class Downloading(
        override val ref: ImageReference
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
        override val ref: ImageReference,
        val throwable: Throwable
    ) : ImageDownloadState

    data class Done(
        override val ref: ImageReference
    ) : ImageDownloadState
}