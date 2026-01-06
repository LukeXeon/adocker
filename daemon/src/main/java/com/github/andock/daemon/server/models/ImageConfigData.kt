package com.github.andock.daemon.server.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ImageConfigData(
    @SerialName("Hostname")
    val hostname: String = "",
    @SerialName("Domainname")
    val domainname: String = "",
    @SerialName("User")
    val user: String = "",
    @SerialName("AttachStdin")
    val attachStdin: Boolean = false,
    @SerialName("AttachStdout")
    val attachStdout: Boolean = false,
    @SerialName("AttachStderr")
    val attachStderr: Boolean = false,
    @SerialName("Tty")
    val tty: Boolean = false,
    @SerialName("OpenStdin")
    val openStdin: Boolean = false,
    @SerialName("StdinOnce")
    val stdinOnce: Boolean = false,
    @SerialName("Env")
    val env: List<String> = emptyList(),
    @SerialName("Cmd")
    val cmd: List<String> = emptyList(),
    @SerialName("Image")
    val image: String = "",
    @SerialName("Volumes")
    val volumes: Map<String, Map<String, String>>? = null,
    @SerialName("WorkingDir")
    val workingDir: String = "",
    @SerialName("Entrypoint")
    val entrypoint: List<String>? = null,
    @SerialName("OnBuild")
    val onBuild: List<String>? = null,
    @SerialName("Labels")
    val labels: Map<String, String>? = null
)