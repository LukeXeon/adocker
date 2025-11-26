package com.github.adocker.daemon.images

enum class PullStatus {
    WAITING,
    DOWNLOADING,
    EXTRACTING,
    DONE,
    ERROR
}