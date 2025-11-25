package com.github.adocker.core.repository.model

enum class PullStatus {
    WAITING,
    DOWNLOADING,
    EXTRACTING,
    DONE,
    ERROR
}