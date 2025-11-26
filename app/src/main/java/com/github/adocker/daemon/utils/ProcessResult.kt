package com.github.adocker.daemon.utils

data class ProcessResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
)