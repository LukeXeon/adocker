package com.adocker.runner.domain.model

/**
 * Container execution result
 */
data class ExecResult(
    val exitCode: Int,
    val output: String
)