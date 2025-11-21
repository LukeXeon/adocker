package com.adocker.runner.data.remote.api

import com.adocker.runner.core.config.Config
import com.adocker.runner.core.config.RegistrySettings
import com.adocker.runner.data.remote.dto.*
import com.adocker.runner.domain.model.ImageReference
import com.adocker.runner.domain.model.Layer
import com.adocker.runner.domain.model.PullProgress
import com.adocker.runner.domain.model.PullStatus
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

/**
 * Docker Registry API client - equivalent to udocker's DockerIoAPI
 */
class DockerRegistryApi {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            level = LogLevel.HEADERS
        }
        install(HttpTimeout) {
            requestTimeoutMillis = Config.NETWORK_TIMEOUT
            connectTimeoutMillis = Config.NETWORK_TIMEOUT
            socketTimeoutMillis = Config.DOWNLOAD_TIMEOUT
        }
        defaultRequest {
            header(HttpHeaders.UserAgent, "${Config.APP_NAME}/${Config.VERSION}")
        }
    }

    private var authToken: String? = null
    private var tokenExpiry: Long = 0

    /**
     * Get authentication token for Docker Hub
     */
    suspend fun authenticate(
        repository: String,
        registry: String = Config.DEFAULT_REGISTRY
    ): Result<String> = runCatching {
        val authUrl = when {
            registry.contains("docker.io") || registry == Config.DEFAULT_REGISTRY -> {
                "${Config.DEFAULT_REGISTRY_AUTH}/token?service=${Config.DEFAULT_REGISTRY_SERVICE}&scope=repository:$repository:pull"
            }
            else -> {
                "$registry/v2/token?service=registry&scope=repository:$repository:pull"
            }
        }

        val response: AuthTokenResponse = client.get(authUrl).body()
        authToken = response.token
        tokenExpiry = System.currentTimeMillis() + ((response.expiresIn ?: 300) * 1000L)
        response.token
    }

    /**
     * Check if current token is valid
     */
    private fun isTokenValid(): Boolean {
        return authToken != null && System.currentTimeMillis() < tokenExpiry
    }

    /**
     * Get manifest for an image (handles both manifest list and image manifest)
     */
    suspend fun getManifest(
        imageRef: ImageReference,
        architecture: String = Config.getArchitecture()
    ): Result<ImageManifestV2> = runCatching {
        val registry = getRegistryUrl(imageRef.registry)

        if (!isTokenValid()) {
            authenticate(imageRef.repository, registry).getOrThrow()
        }

        // First try to get the manifest list
        val manifestListResponse = client.get("$registry/v2/${imageRef.repository}/manifests/${imageRef.tag}") {
            header(HttpHeaders.Authorization, "Bearer $authToken")
            header(HttpHeaders.Accept, listOf(
                "application/vnd.docker.distribution.manifest.list.v2+json",
                "application/vnd.oci.image.index.v1+json",
                "application/vnd.docker.distribution.manifest.v2+json",
                "application/vnd.oci.image.manifest.v1+json"
            ).joinToString(", "))
        }

        val contentType = manifestListResponse.contentType()?.toString() ?: ""
        val bodyText = manifestListResponse.bodyAsText()

        when {
            contentType.contains("manifest.list") || contentType.contains("image.index") -> {
                // It's a manifest list, find the right architecture
                val manifestList: ManifestListResponse = json.decodeFromString(bodyText)
                val platformManifest = manifestList.manifests?.find { manifest ->
                    manifest.platform?.architecture == architecture &&
                            manifest.platform.os == Config.DEFAULT_OS
                } ?: manifestList.manifests?.firstOrNull()
                ?: throw Exception("No suitable manifest found for $architecture")

                // Get the specific manifest
                getManifestByDigest(imageRef, platformManifest.digest).getOrThrow()
            }
            else -> {
                // Direct manifest
                json.decodeFromString(bodyText)
            }
        }
    }

    /**
     * Get manifest by digest
     */
    suspend fun getManifestByDigest(
        imageRef: ImageReference,
        digest: String
    ): Result<ImageManifestV2> = runCatching {
        val registry = getRegistryUrl(imageRef.registry)

        if (!isTokenValid()) {
            authenticate(imageRef.repository, registry).getOrThrow()
        }

        val response = client.get("$registry/v2/${imageRef.repository}/manifests/$digest") {
            header(HttpHeaders.Authorization, "Bearer $authToken")
            header(HttpHeaders.Accept, listOf(
                "application/vnd.docker.distribution.manifest.v2+json",
                "application/vnd.oci.image.manifest.v1+json"
            ).joinToString(", "))
        }

        response.body()
    }

    /**
     * Get image configuration
     */
    suspend fun getImageConfig(
        imageRef: ImageReference,
        configDigest: String
    ): Result<ImageConfigResponse> = runCatching {
        val registry = getRegistryUrl(imageRef.registry)

        if (!isTokenValid()) {
            authenticate(imageRef.repository, registry).getOrThrow()
        }

        val response = client.get("$registry/v2/${imageRef.repository}/blobs/$configDigest") {
            header(HttpHeaders.Authorization, "Bearer $authToken")
        }

        response.body()
    }

    /**
     * Download a layer blob
     */
    suspend fun downloadLayer(
        imageRef: ImageReference,
        layer: Layer,
        destFile: File,
        onProgress: (Long, Long) -> Unit
    ): Result<Unit> = runCatching {
        val registry = getRegistryUrl(imageRef.registry)

        if (!isTokenValid()) {
            authenticate(imageRef.repository, registry).getOrThrow()
        }

        client.prepareGet("$registry/v2/${imageRef.repository}/blobs/${layer.digest}") {
            header(HttpHeaders.Authorization, "Bearer $authToken")
            timeout {
                requestTimeoutMillis = Config.DOWNLOAD_TIMEOUT
            }
        }.execute { response ->
            val contentLength = response.contentLength() ?: layer.size
            var downloaded = 0L

            destFile.parentFile?.mkdirs()
            FileOutputStream(destFile).use { fos ->
                val channel: ByteReadChannel = response.body()
                val buffer = ByteArray(Config.DOWNLOAD_BUFFER_SIZE)
                while (!channel.isClosedForRead) {
                    val bytesRead = channel.readAvailable(buffer)
                    if (bytesRead > 0) {
                        fos.write(buffer, 0, bytesRead)
                        downloaded += bytesRead
                        onProgress(downloaded, contentLength)
                    }
                }
            }
        }
    }

    /**
     * Pull an image with progress updates
     */
    fun pullImage(imageRef: ImageReference): Flow<PullProgress> = flow {
        val manifest = getManifest(imageRef).getOrThrow()
        val layers = manifest.layers.map { layer ->
            Layer(
                digest = layer.digest,
                size = layer.size,
                mediaType = layer.mediaType
            )
        }

        // Download each layer
        for (layer in layers) {
            emit(PullProgress(layer.digest, 0, layer.size, PullStatus.DOWNLOADING))

            val layerFile = File(Config.layersDir, "${layer.digest.removePrefix("sha256:")}.tar.gz")

            if (!layerFile.exists()) {
                downloadLayer(imageRef, layer, layerFile) { downloaded, total ->
                    // Progress callback handled in calling code
                }.getOrThrow()
            }

            emit(PullProgress(layer.digest, layer.size, layer.size, PullStatus.DONE))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Search Docker Hub for images
     */
    suspend fun search(query: String, limit: Int = 25): Result<List<SearchResultDto>> = runCatching {
        val response: SearchResponse = client.get("https://hub.docker.com/v2/search/repositories/") {
            parameter("query", query)
            parameter("page_size", limit)
        }.body()

        response.results
    }

    /**
     * Get tags for a repository
     */
    suspend fun getTags(imageRef: ImageReference): Result<List<String>> = runCatching {
        val registry = getRegistryUrl(imageRef.registry)

        if (!isTokenValid()) {
            authenticate(imageRef.repository, registry).getOrThrow()
        }

        val response: TagsListResponse = client.get("$registry/v2/${imageRef.repository}/tags/list") {
            header(HttpHeaders.Authorization, "Bearer $authToken")
        }.body()

        response.tags
    }

    private fun getRegistryUrl(registry: String): String {
        return runBlocking {
            RegistrySettings.getRegistryUrl(registry)
        }
    }

    /**
     * Get current mirror info for display
     */
    suspend fun getCurrentMirrorName(): String {
        return RegistrySettings.getCurrentMirror().name
    }

    fun close() {
        client.close()
    }
}
