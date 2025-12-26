package com.github.andock.daemon.images

sealed interface ImageOperation {
    object Remove : ImageOperation
}