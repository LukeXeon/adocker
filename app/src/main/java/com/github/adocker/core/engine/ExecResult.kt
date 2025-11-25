package com.github.adocker.core.engine

/**
 * Container execution result
 */
data class ExecResult(
    val exitCode: Int,
    val output: String
)