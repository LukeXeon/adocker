package com.adocker.runner.data.remote.api

import com.adocker.runner.core.config.AppConfig
import timber.log.Timber
import com.adocker.runner.core.config.RegistrySettingsManager
import com.adocker.runner.data.remote.dto.*
import com.adocker.runner.domain.model.ImageReference
import com.adocker.runner.domain.model.Layer
import com.adocker.runner.domain.model.PullProgress
import com.adocker.runner.domain.model.PullStatus
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Docker Registry API client - equivalent to udocker's DockerIoAPI
 */
@Singleton
class DockerRegistryApi @Inject constructor(
    private val registrySettings: RegistrySettingsManager,
    private val appConfig: AppConfig,
    private val json: Json,
    private val client: HttpClient,
) {

    private var authToken: String? = null
    private var tokenExpiry: Long = 0

    /**
     * Get authentication token for Docker Hub or mirror
     *
     * Implements Docker Registry V2 authentication flow:
     * 1. Try to access /v2/ endpoint without auth to get WWW-Authenticate header
     * 2. Parse the WWW-Authenticate header to extract auth service URL
     * 3. Request anonymous token from auth service
     * 4. Use token for subsequent requests
     */
    suspend fun authenticate(
        repository: String,
        registry: String = AppConfig.DEFAULT_REGISTRY
    ): Result<String> = runCatching {
        Timber.d("Authenticating for repository: $repository, registry: $registry")

        try {
            // Step 1: Try to access /v2/ without auth to get WWW-Authenticate header
            val pingResponse = client.get("$registry/v2/") {
                // Don't follow redirects, we want to see 401
            }

            Timber.d("Ping response status: ${pingResponse.status}")

            // If 200 OK, no auth needed (shouldn't happen but handle it)
            if (pingResponse.status == HttpStatusCode.OK) {
                Timber.i("Registry allows anonymous access")
                authToken = ""
                tokenExpiry = System.currentTimeMillis() + 3600000
                return@runCatching ""
            }

            // Step 2: Parse WWW-Authenticate header
            val wwwAuth = pingResponse.headers["Www-Authenticate"]
                ?: pingResponse.headers["WWW-Authenticate"]
                ?: throw Exception("No WWW-Authenticate header in response")

            Timber.d("WWW-Authenticate: $wwwAuth")

            // Parse: Bearer realm="https://xxx",service="xxx",scope="..."
            val realm = Regex("realm=\"([^\"]+)\"").find(wwwAuth)?.groupValues?.get(1)
                ?: throw Exception("Cannot parse realm from WWW-Authenticate")
            val service = Regex("service=\"([^\"]+)\"").find(wwwAuth)?.groupValues?.get(1) ?: ""

            // Step 3: Build auth URL and request token
            val authUrl = buildString {
                append(realm)
                append("?service=").append(service)
                append("&scope=repository:$repository:pull")
            }

            Timber.d("Requesting token from: $authUrl")

            val tokenResponse = client.get(authUrl).body<AuthTokenResponse>()
            authToken = tokenResponse.token ?: tokenResponse.accessToken
            tokenExpiry = System.currentTimeMillis() + tokenResponse.expiresIn * 1000L

            Timber.i("Successfully obtained auth token (expires in ${tokenResponse.expiresIn}s)")

            authToken ?: ""
        } catch (e: Exception) {
            Timber.e(e, "Authentication failed for $registry: ${e.message}")
            throw e
        }
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
        architecture: String = appConfig.architecture
    ): Result<ImageManifestV2> = runCatching {
        val registry = getRegistryUrl(imageRef.registry)

        if (!isTokenValid()) {
            authenticate(imageRef.repository, registry).getOrThrow()
        }

        // First try to get the manifest list
        val manifestListResponse = client.get(
            "$registry/v2/${imageRef.repository}/manifests/${imageRef.tag}"
        ) {
            // Only add Authorization header if we have a token
            if (!authToken.isNullOrEmpty()) {
                header(HttpHeaders.Authorization, "Bearer $authToken")
            }
            header(
                HttpHeaders.Accept, listOf(
                    "application/vnd.docker.distribution.manifest.list.v2+json",
                    "application/vnd.oci.image.index.v1+json",
                    "application/vnd.docker.distribution.manifest.v2+json",
                    "application/vnd.oci.image.manifest.v1+json"
                ).joinToString(", ")
            )
        }

        val contentType = manifestListResponse.contentType()?.toString() ?: ""
        val bodyText = manifestListResponse.bodyAsText()

        Timber.d("Manifest response - ContentType: $contentType")
        Timber.d("Manifest response - Body: ${bodyText.take(500)}")

        when {
            contentType.contains("manifest.list") || contentType.contains("image.index") -> {
                // It's a manifest list, find the right architecture
                val manifestList: ManifestListResponse = json.decodeFromString(bodyText)
                val platformManifest = manifestList.manifests?.find { manifest ->
                    manifest.platform?.architecture == architecture &&
                            manifest.platform.os == AppConfig.DEFAULT_OS
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
            if (!authToken.isNullOrEmpty()) {
                header(HttpHeaders.Authorization, "Bearer $authToken")
            }
            header(
                HttpHeaders.Accept, listOf(
                    "application/vnd.docker.distribution.manifest.v2+json",
                    "application/vnd.oci.image.manifest.v1+json"
                ).joinToString(", ")
            )
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
            if (!authToken.isNullOrEmpty()) {
                header(HttpHeaders.Authorization, "Bearer $authToken")
            }
        }

        // Some registries (like DaoCloud) don't set ContentType header,
        // so manually parse JSON from body text
        val bodyText = response.bodyAsText()
        json.decodeFromString(bodyText)
    }

    /**
     * Download a layer blob
     */
    suspend fun downloadLayer(
        imageRef: ImageReference,
        layer: Layer,
        destFile: File,
        onProgress: (Long, Long) -> Unit
    ): Result<Unit> {
        return runCatching {
            val registry = getRegistryUrl(imageRef.registry)

            if (!isTokenValid()) {
                authenticate(imageRef.repository, registry).getOrThrow()
            }

            Timber.d("Starting layer download: ${layer.digest.take(16)}, size: ${layer.size}")

            client.prepareGet("$registry/v2/${imageRef.repository}/blobs/${layer.digest}") {
                if (!authToken.isNullOrEmpty()) {
                    header(HttpHeaders.Authorization, "Bearer $authToken")
                }
                timeout {
                    requestTimeoutMillis = AppConfig.DOWNLOAD_TIMEOUT
                }
            }.execute { response ->
                Timber.d("Layer download response status: ${response.status}")
                val contentLength = response.contentLength() ?: layer.size
                var downloaded = 0L

                destFile.parentFile?.mkdirs()
                FileOutputStream(destFile).use { fos ->
                    val channel: ByteReadChannel = response.body()
                    val buffer = ByteArray(AppConfig.DOWNLOAD_BUFFER_SIZE)
                    while (!channel.isClosedForRead) {
                        val bytesRead = channel.readAvailable(buffer)
                        if (bytesRead > 0) {
                            fos.write(buffer, 0, bytesRead)
                            downloaded += bytesRead
                            onProgress(downloaded, contentLength)
                            if (downloaded % (512 * 1024) == 0L || downloaded == contentLength) {
                                Timber.d("Download progress: $downloaded/$contentLength bytes")
                            }
                        }
                    }
                }
                Timber.i("Layer download completed: ${layer.digest.take(16)}, downloaded: $downloaded bytes")
            }
            Unit
        }.onFailure { e ->
            Timber.e(e, "Layer download failed: ${layer.digest.take(16)}")
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

            val layerFile =
                File(appConfig.layersDir, "${layer.digest.removePrefix("sha256:")}.tar.gz")

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
    suspend fun search(query: String, limit: Int = 25): Result<List<SearchResultDto>> =
        runCatching {
            val response: SearchResponse =
                client.get("https://hub.docker.com/v2/search/repositories/") {
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

        val response: TagsListResponse =
            client.get("$registry/v2/${imageRef.repository}/tags/list") {
                if (!authToken.isNullOrEmpty()) {
                    header(HttpHeaders.Authorization, "Bearer $authToken")
                }
            }.body()

        response.tags
    }

    private suspend fun getRegistryUrl(registry: String): String {
        return registrySettings.getRegistryUrl(registry)
    }

    /**
     * Get current mirror info for display
     */
    suspend fun getCurrentMirrorName(): String {
        return registrySettings.getCurrentMirror().name
    }
}
