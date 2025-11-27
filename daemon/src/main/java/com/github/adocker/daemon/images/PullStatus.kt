package com.github.adocker.daemon.images

enum class PullStatus {
    WAITING,
    DOWNLOADING,
    DONE,
    ERROR
}