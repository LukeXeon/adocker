package com.github.andock.daemon.images.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Image Configuration - Container Execution Parameters
 *
 * Defines the default execution parameters for containers created from this image.
 * These settings can be overridden when starting a container.
 * Conforms to: OCI Image Specification / Docker Image Spec v1.2
 *
 * Field names use Docker's capitalized convention for compatibility.
 *
 * @property cmd Default arguments to entrypoint. If entrypoint is null, this is the command to execute.
 * @property entrypoint Command to execute when container starts. Cannot be overridden by docker run args.
 * @property env Environment variables in "NAME=VALUE" format
 * @property workingDir Current working directory for commands
 * @property user Username or UID (and optional GID) for process execution (e.g., "user:group", "1000:1000")
 * @property exposedPorts Ports to expose in format "port/protocol" (e.g., "80/tcp", "53/udp")
 * @property volumes Mount points for container-specific data (keys are paths like "/data", "/var/log")
 * @property labels Arbitrary metadata key-value pairs following OCI annotation rules
 */
@Serializable
data class ImageConfig(
    @SerialName("Cmd") val cmd: List<String>? = null,
    @SerialName("Entrypoint") val entrypoint: List<String>? = null,
    @SerialName("Env") val env: List<String>? = null,
    @SerialName("WorkingDir") val workingDir: String? = null,
    @SerialName("User") val user: String? = null,
    @SerialName("ExposedPorts") val exposedPorts: Map<String, @Serializable(with = EmptyObjectSerializer::class) Unit>? = null,
    @SerialName("Volumes") val volumes: Map<String, @Serializable(with = EmptyObjectSerializer::class) Unit>? = null,
    @SerialName("Labels") val labels: Map<String, String>? = null
)