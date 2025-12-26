package com.github.andock.daemon.images

sealed interface ImageState {
    val id: String

    data class Downloaded(override val id: String) : ImageState
    data class Removing(override val id: String) : ImageState

    data class Removed(override val id: String) : ImageState
}