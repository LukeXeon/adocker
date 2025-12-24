package com.github.andock.daemon.images

sealed interface ImageState {
    val id: String

    data class Downloading(override val id: String) : ImageState

    data class Downloaded(override val id: String) : ImageState
}