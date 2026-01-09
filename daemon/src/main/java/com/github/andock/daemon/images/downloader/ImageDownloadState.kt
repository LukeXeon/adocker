package com.github.andock.daemon.images.downloader

import com.github.andock.daemon.images.ImageReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed interface ImageDownloadState {
    val ref: ImageReference

    class Downloading(
        override val ref: ImageReference
    ) : ImageDownloadState {
        private val _steps = MutableStateFlow<List<ImageDownloadStep>>(emptyList())

        val steps = _steps.asStateFlow()

        internal suspend inline fun <T> step(
            name: String,
            block: suspend MutableStateFlow<Float>.() -> T
        ): T {
            var exception: Exception? = null
            val progress = MutableStateFlow(0f)
            val step = ImageDownloadStep(name, progress)
            _steps.update {
                buildList(it.size + 1) {
                    addAll(it)
                    add(step)
                }
            }
            try {
                return progress.block()
            } catch (e: Exception) {
                exception = e
                throw e
            } finally {
                if (exception == null) {
                    progress.value = 1f
                }
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