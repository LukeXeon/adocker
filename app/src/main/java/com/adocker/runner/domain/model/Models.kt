package com.adocker.runner.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Docker image reference
 */
data class ImageReference(
    val registry: String = "docker.io",
    val repository: String,
    val tag: String = "latest",
    val digest: String? = null
) {
    val fullName: String
        get() = buildString {
            if (registry != "docker.io") {
                append(registry)
                append("/")
            }
            append(repository)
            append(":")
            append(tag)
        }

    companion object {
        fun parse(imageName: String): ImageReference {
            var name = imageName
            var registry = "docker.io"
            var tag = "latest"
            var digest: String? = null

            // Check for digest
            if (name.contains("@sha256:")) {
                val parts = name.split("@sha256:")
                name = parts[0]
                digest = "sha256:${parts[1]}"
            }

            // Check for tag
            if (name.contains(":") && !name.contains("/")) {
                val parts = name.split(":")
                name = parts[0]
                tag = parts[1]
            } else if (name.contains(":")) {
                val lastColon = name.lastIndexOf(":")
                val afterColon = name.substring(lastColon + 1)
                if (!afterColon.contains("/")) {
                    tag = afterColon
                    name = name.substring(0, lastColon)
                }
            }

            // Check for registry
            if (name.contains("/")) {
                val firstSlash = name.indexOf("/")
                val possibleRegistry = name.substring(0, firstSlash)
                if (possibleRegistry.contains(".") || possibleRegistry.contains(":")) {
                    registry = possibleRegistry
                    name = name.substring(firstSlash + 1)
                }
            }

            // Add library prefix for official images
            val repository = if (!name.contains("/") && registry == "docker.io") {
                "library/$name"
            } else {
                name
            }

            return ImageReference(registry, repository, tag, digest)
        }
    }
}

/**
 * Local image stored on device
 */
@Serializable
data class LocalImage(
    val id: String = UUID.randomUUID().toString(),
    val repository: String,
    val tag: String,
    val digest: String,
    val architecture: String,
    val os: String,
    val created: Long = System.currentTimeMillis(),
    val size: Long = 0,
    val layerIds: List<String> = emptyList(),
    val config: ImageConfig? = null
) {
    val fullName: String get() = "$repository:$tag"
}

/**
 * Image configuration from manifest
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

/**
 * Serializer for empty JSON objects
 */
object EmptyObjectSerializer : kotlinx.serialization.KSerializer<Unit> {
    override val descriptor = kotlinx.serialization.descriptors.buildClassSerialDescriptor("EmptyObject")
    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: Unit) {
        encoder.beginStructure(descriptor).endStructure(descriptor)
    }
    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): Unit {
        decoder.beginStructure(descriptor).endStructure(descriptor)
    }
}

/**
 * Container definition
 */
@Serializable
data class Container(
    val id: String = UUID.randomUUID().toString().take(12),
    val name: String,
    val imageId: String,
    val imageName: String,
    val created: Long = System.currentTimeMillis(),
    val status: ContainerStatus = ContainerStatus.CREATED,
    val config: ContainerConfig = ContainerConfig(),
    val pid: Int? = null
)

/**
 * Container runtime status
 */
@Serializable
enum class ContainerStatus {
    CREATED,
    RUNNING,
    PAUSED,
    STOPPED,
    EXITED
}

/**
 * Container configuration
 */
@Serializable
data class ContainerConfig(
    val cmd: List<String> = listOf("/bin/sh"),
    val entrypoint: List<String>? = null,
    val env: Map<String, String> = emptyMap(),
    val workingDir: String = "/",
    val user: String = "root",
    val hostname: String = "localhost",
    val binds: List<VolumeBinding> = emptyList(),
    val portBindings: Map<String, String> = emptyMap(),
    val execMode: String = "P1",
    val networkEnabled: Boolean = false
)

/**
 * Volume binding (host:container)
 */
@Serializable
data class VolumeBinding(
    val hostPath: String,
    val containerPath: String,
    val readOnly: Boolean = false
)

/**
 * Layer information
 */
@Serializable
data class Layer(
    val digest: String,
    val size: Long,
    val mediaType: String,
    val downloaded: Boolean = false,
    val extracted: Boolean = false
)

/**
 * Image pull progress
 */
data class PullProgress(
    val layerDigest: String,
    val downloaded: Long,
    val total: Long,
    val status: PullStatus
)

enum class PullStatus {
    WAITING,
    DOWNLOADING,
    EXTRACTING,
    DONE,
    ERROR
}

/**
 * Search result from Docker Hub
 */
@Serializable
data class SearchResult(
    val name: String,
    val description: String = "",
    @SerialName("star_count") val starCount: Int = 0,
    @SerialName("is_official") val isOfficial: Boolean = false,
    @SerialName("is_automated") val isAutomated: Boolean = false
)

/**
 * Container execution result
 */
data class ExecResult(
    val exitCode: Int,
    val output: String
)
