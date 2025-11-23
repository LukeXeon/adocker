package com.adocker.runner.data.remote.api

import android.util.Log
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
        registry: String = Config.DEFAULT_REGISTRY
    ): Result<String> = runCatching {
        Log.d("DockerRegistryApi", "Authenticating for repository: $repository, registry: $registry")

        try {
            // Step 1: Try to access /v2/ without auth to get WWW-Authenticate header
            val pingResponse = client.get("$registry/v2/") {
                // Don't follow redirects, we want to see 401
            }

            Log.d("DockerRegistryApi", "Ping response status: ${pingResponse.status}")

            // If 200 OK, no auth needed (shouldn't happen but handle it)
            if (pingResponse.status == io.ktor.http.HttpStatusCode.OK) {
                Log.i("DockerRegistryApi", "Registry allows anonymous access")
                authToken = ""
                tokenExpiry = System.currentTimeMillis() + 3600000
                return@runCatching ""
            }

            // Step 2: Parse WWW-Authenticate header
            val wwwAuth = pingResponse.headers["Www-Authenticate"]
                ?: pingResponse.headers["WWW-Authenticate"]
                ?: throw Exception("No WWW-Authenticate header in response")

            Log.d("DockerRegistryApi", "WWW-Authenticate: $wwwAuth")

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

            Log.d("DockerRegistryApi", "Requesting token from: $authUrl")

            val tokenResponse: AuthTokenResponse = client.get(authUrl).body()
            authToken = tokenResponse.token ?: tokenResponse.access_token
            tokenExpiry = System.currentTimeMillis() + ((tokenResponse.expiresIn ?: tokenResponse.expires_in ?: 300) * 1000L)

            Log.i("DockerRegistryApi", "Successfully obtained auth token (expires in ${tokenResponse.expiresIn ?: tokenResponse.expires_in ?: 300}s)")

            authToken ?: ""
        } catch (e: Exception) {
            Log.e("DockerRegistryApi", "Authentication failed for $registry: ${e.message}", e)
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
        architecture: String = Config.getArchitecture()
    ): Result<ImageManifestV2> = runCatching {
        val registry = getRegistryUrl(imageRef.registry)

        if (!isTokenValid()) {
            authenticate(imageRef.repository, registry).getOrThrow()
        }

        // First try to get the manifest list
        val manifestListResponse = client.get("$registry/v2/${imageRef.repository}/manifests/${imageRef.tag}") {
            // Only add Authorization header if we have a token
            if (!authToken.isNullOrEmpty()) {
                header(HttpHeaders.Authorization, "Bearer $authToken")
            }
            header(HttpHeaders.Accept, listOf(
                "application/vnd.docker.distribution.manifest.list.v2+json",
                "application/vnd.oci.image.index.v1+json",
                "application/vnd.docker.distribution.manifest.v2+json",
                "application/vnd.oci.image.manifest.v1+json"
            ).joinToString(", "))
        }

        val contentType = manifestListResponse.contentType()?.toString() ?: ""
        val bodyText = manifestListResponse.bodyAsText()

        Log.d("DockerRegistryApi", "Manifest response - ContentType: $contentType")
        Log.d("DockerRegistryApi", "Manifest response - Body: ${bodyText.take(500)}")

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
            if (!authToken.isNullOrEmpty()) {
                header(HttpHeaders.Authorization, "Bearer $authToken")
            }
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

            Log.d("DockerRegistryApi", "Starting layer download: ${layer.digest.take(16)}, size: ${layer.size}")

            client.prepareGet("$registry/v2/${imageRef.repository}/blobs/${layer.digest}") {
                if (!authToken.isNullOrEmpty()) {
                    header(HttpHeaders.Authorization, "Bearer $authToken")
                }
                timeout {
                    requestTimeoutMillis = Config.DOWNLOAD_TIMEOUT
                }
            }.execute { response ->
                Log.d("DockerRegistryApi", "Layer download response status: ${response.status}")
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
                            if (downloaded % (512 * 1024) == 0L || downloaded == contentLength) {
                                Log.d("DockerRegistryApi", "Download progress: $downloaded/$contentLength bytes")
                            }
                        }
                    }
                }
                Log.i("DockerRegistryApi", "Layer download completed: ${layer.digest.take(16)}, downloaded: $downloaded bytes")
            }
            Unit
        }.onFailure { e ->
            Log.e("DockerRegistryApi", "Layer download failed: ${layer.digest.take(16)}", e)
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
            if (!authToken.isNullOrEmpty()) {
                header(HttpHeaders.Authorization, "Bearer $authToken")
            }
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
