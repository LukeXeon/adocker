package com.github.andock.daemon.server.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExecCreateRequest(
    @SerialName("AttachStdin")
    val attachStdin: Boolean = false,
    @SerialName("AttachStdout")
    val attachStdout: Boolean = true,
    @SerialName("AttachStderr")
    val attachStderr: Boolean = true,
    @SerialName("DetachKeys")
    val detachKeys: String? = null,
    @SerialName("Tty")
    val tty: Boolean = false,
    @SerialName("Env")
    val env: List<String>? = null,
    @SerialName("Cmd")
    val cmd: List<String>,
    @SerialName("Privileged")
    val privileged: Boolean = false,
    @SerialName("User")
    val user: String? = null,
    @SerialName("WorkingDir")
    val workingDir: String? = null
)