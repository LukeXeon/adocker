package com.github.adocker.core.image

enum class PullStatus {
    WAITING,
    DOWNLOADING,
    EXTRACTING,
    DONE,
    ERROR
}