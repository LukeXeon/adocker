package com.github.andock.daemon.images

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface ImageDownloadState {

    class Downloading() : ImageDownloadState {
        private val mutex = Mutex()
        private val _progress = MutableStateFlow<Map<String, Progress>>(
            mapOf(
                "manifest" to Progress(0, 1)
            )
        )

        internal suspend inline fun update(function: (Map<String, Progress>) -> Map<String, Progress>) {
            mutex.withLock {
                _progress.update(function)
            }
        }

        val progress = _progress.asStateFlow()

        data class Progress(
            val downloaded: Long,
            val total: Long,
        )
    }

    data class Error(val throwable: Throwable) : ImageDownloadState

    object Done : ImageDownloadState
}