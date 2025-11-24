package com.adocker.runner.engine.proot

/**
 * Container execution result
 */
data class ExecResult(
    val exitCode: Int,
    val output: String
)