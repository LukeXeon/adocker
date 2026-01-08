package com.github.andock.daemon.containers.creator

import com.github.andock.daemon.containers.Container
import com.github.andock.daemon.images.DownloadProgress
import com.github.andock.daemon.images.models.ContainerConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface ContainerCreateState {
    val id: String

    class Creating(
        override val id: String,
        val imageId: String,
        val name: String?,
        val config: ContainerConfig
    ) : ContainerCreateState {
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
        override val id: String,
        val throwable: Throwable
    ) : ContainerCreateState

    data class Done(
        val container: Container
    ) : ContainerCreateState {
        override val id
            get() = container.id
    }
}