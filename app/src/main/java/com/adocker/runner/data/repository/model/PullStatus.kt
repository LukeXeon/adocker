package com.adocker.runner.data.repository.model

enum class PullStatus {
    WAITING,
    DOWNLOADING,
    EXTRACTING,
    DONE,
    ERROR
}