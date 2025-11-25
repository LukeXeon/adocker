package com.github.adocker.core.utils

data class ProcessResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
)