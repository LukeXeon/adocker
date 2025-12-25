package com.github.andock.daemon.images

sealed interface ImageOperation {
    object Download : ImageOperation
    object Remove : ImageOperation
}