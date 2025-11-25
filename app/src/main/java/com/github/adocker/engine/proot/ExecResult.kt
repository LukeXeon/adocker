package com.github.adocker.engine.proot

/**
 * Container execution result
 */
data class ExecResult(
    val exitCode: Int,
    val output: String
)