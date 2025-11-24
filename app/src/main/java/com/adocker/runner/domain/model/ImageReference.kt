package com.adocker.runner.domain.model

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