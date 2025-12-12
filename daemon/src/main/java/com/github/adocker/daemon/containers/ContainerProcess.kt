package com.github.adocker.daemon.containers

import kotlinx.coroutines.Deferred
import java.io.BufferedWriter
import java.io.File

data class ContainerProcess(
    val job: Deferred<Int>,
    val writer: BufferedWriter,
    val stdout: File?,
    val stderr: File?
)