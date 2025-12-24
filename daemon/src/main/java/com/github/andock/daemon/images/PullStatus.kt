package com.github.andock.daemon.images

enum class PullStatus {
    WAITING,
    DOWNLOADING,
    DONE,
    ERROR
}