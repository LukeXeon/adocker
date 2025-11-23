package com.adocker.runner.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Docker Registry API DTOs
 */

@Serializable
data class AuthTokenResponse(
    val token: String? = null,
    @SerialName("access_token") val access_token: String? = null,
    @SerialName("expires_in") val expires_in: Int? = null,
    val expiresIn: Int? = null,
    @SerialName("issued_at") val issuedAt: String? = null
)

@Serializable
data class ManifestListResponse(
    val schemaVersion: Int,
    val mediaType: String? = null,
    val manifests: List<ManifestDescriptor>? = null,
    // For v2 schema 1
    val config: ConfigDescriptor? = null,
    val layers: List<LayerDescriptor>? = null
)

@Serializable
data class ManifestDescriptor(
    val mediaType: String,
    val digest: String,
    val size: Long,
    val platform: Platform? = null
)

@Serializable
data class Platform(
    val architecture: String,
    val os: String,
    val variant: String? = null,
    @SerialName("os.version") val osVersion: String? = null
)

@Serializable
data class ImageManifestV2(
    val schemaVersion: Int,
    val mediaType: String? = null,
    val config: ConfigDescriptor,
    val layers: List<LayerDescriptor>
)

@Serializable
data class ConfigDescriptor(
    val mediaType: String,
    val digest: String,
    val size: Long
)

@Serializable
data class LayerDescriptor(
    val mediaType: String,
    val digest: String,
    val size: Long,
    val urls: List<String>? = null
)

@Serializable
data class ImageConfigResponse(
    val architecture: String? = null,
    val os: String? = null,
    val config: ContainerConfigDto? = null,
    val rootfs: RootfsDto? = null,
    val history: List<HistoryDto>? = null,
    val created: String? = null
)

@Serializable
data class ContainerConfigDto(
    @SerialName("Cmd") val cmd: List<String>? = null,
    @SerialName("Entrypoint") val entrypoint: List<String>? = null,
    @SerialName("Env") val env: List<String>? = null,
    @SerialName("WorkingDir") val workingDir: String? = null,
    @SerialName("User") val user: String? = null,
    @SerialName("ExposedPorts") val exposedPorts: Map<String, EmptyObject>? = null,
    @SerialName("Volumes") val volumes: Map<String, EmptyObject>? = null,
    @SerialName("Labels") val labels: Map<String, String>? = null
)

@Serializable
class EmptyObject

@Serializable
data class RootfsDto(
    val type: String,
    @SerialName("diff_ids") val diffIds: List<String>
)

@Serializable
data class HistoryDto(
    val created: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("empty_layer") val emptyLayer: Boolean? = null,
    val comment: String? = null
)

// Docker Hub Search API
@Serializable
data class SearchResponse(
    val count: Int,
    val next: String? = null,
    val previous: String? = null,
    val results: List<SearchResultDto>
)

@Serializable
data class SearchResultDto(
    val name: String,
    val description: String? = null,
    @SerialName("star_count") val starCount: Int = 0,
    @SerialName("is_official") val isOfficial: Boolean = false,
    @SerialName("is_automated") val isAutomated: Boolean = false,
    @SerialName("pull_count") val pullCount: Long = 0
)

// Tags API
@Serializable
data class TagsListResponse(
    val name: String,
    val tags: List<String>
)

// Catalog API
@Serializable
data class CatalogResponse(
    val repositories: List<String>
)

// Error response
@Serializable
data class RegistryErrorResponse(
    val errors: List<RegistryError>
)

@Serializable
data class RegistryError(
    val code: String,
    val message: String,
    val detail: String? = null
)
