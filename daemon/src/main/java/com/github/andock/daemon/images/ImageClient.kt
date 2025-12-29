package com.github.andock.daemon.images

import com.github.andock.daemon.app.AppContext
import com.github.andock.daemon.images.model.AuthTokenResponse
import com.github.andock.daemon.images.model.ImageConfigResponse
import com.github.andock.daemon.images.model.ImageManifestV2
import com.github.andock.daemon.images.model.ManifestListResponse
import com.github.andock.daemon.database.dao.AuthTokenDao
import com.github.andock.daemon.database.dao.RegistryDao
import com.github.andock.daemon.database.model.AuthTokenEntity
import com.github.andock.daemon.database.model.LayerEntity
import com.github.andock.daemon.registries.RegistryManager
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.utils.io.readAvailable
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageClient @Inject constructor(
    private val client: HttpClient,
    private val registryDao: RegistryDao,
    private val registryManager: RegistryManager,
    private val authTokenDao: AuthTokenDao,
    private val json: Json,
) {
    private val realmRegex = Regex("realm=\"([^\"]+)\"")
    private val serviceRegex = Regex("service=\"([^\"]+)\"")

    /**
     * Get authentication token for Docker Hub or mirror
     *
     * Implements Docker Registry V2 authentication flow:
     * 1. Check if mirror has a configured Bearer Token - use it directly if available
     * 2. Otherwise, try to access /v2/ endpoint without auth to get WWW-Authenticate header
     * 3. Parse the WWW-Authenticate header to extract auth service URL
     * 4. Request anonymous token from auth service
     * 5. Use token for subsequent requests
     */
    private suspend fun authenticate(
        repository: String,
        registryUrl: String
    ): Result<String> = runCatching {
        Timber.d("Authenticating for repository: $repository, registry: $registryUrl")
        try {
            // Check if this specific registry URL has a Bearer Token configured
            val bearerToken = registryDao.getBearerTokenByUrl(registryUrl)
            if (!bearerToken.isNullOrEmpty()) {
                Timber.i("Using configured Bearer Token for registry: $registryUrl")
                return@runCatching bearerToken
            }
            // Step 1: Try to access /v2/ without auth to get WWW-Authenticate header
            val pingResponse = client.get("$registryUrl/v2/")
            // Don't follow redirects, we want to see 401
            Timber.d("Ping response status: ${pingResponse.status}")
            // If 200 OK, no auth needed (shouldn't happen but handle it)
            if (pingResponse.status == HttpStatusCode.OK) {
                Timber.i("Registry allows anonymous access")
                return@runCatching ""
            }

            // Step 2: Parse WWW-Authenticate header
            val wwwAuth = pingResponse.headers["Www-Authenticate"]
                ?: pingResponse.headers["WWW-Authenticate"]
                ?: throw IllegalStateException("No WWW-Authenticate header in response")

            Timber.d("WWW-Authenticate: $wwwAuth")

            // Parse: Bearer realm="https://xxx",service="xxx",scope="..."
            val realm = realmRegex.find(wwwAuth)?.groupValues?.get(1)
                ?: throw IllegalStateException("Cannot parse realm from WWW-Authenticate")
            val service = serviceRegex.find(wwwAuth)?.groupValues?.get(1) ?: ""

            // Step 3: Build auth URL and request token
            val authUrl = buildString {
                append(realm)
                append("?service=").append(service)
                append("&scope=repository:$repository:pull")
            }

            val token = authTokenDao.getTokenByUrl(authUrl)
            if (token != null) {
                if (System.currentTimeMillis() < token.expiry) {
                    return@runCatching token.token
                } else {
                    authTokenDao.deleteExpiredTokens()
                }
            }

            Timber.d("Requesting token from: $authUrl")
            val tokenResponse = client.get(authUrl).body<AuthTokenResponse>()
            val authToken = tokenResponse.token ?: tokenResponse.accessToken ?: ""
            authTokenDao.insertToken(
                AuthTokenEntity(
                    authUrl,
                    authToken,
                    System.currentTimeMillis() + tokenResponse.expiresIn * 1000L
                )
            )
            Timber.i("Successfully obtained auth token (expires in ${tokenResponse.expiresIn}s)")
            return@runCatching authToken
        } catch (e: Exception) {
            Timber.e(e, "Authentication failed for registry: $registryUrl")
            throw e
        }
    }

    /**
     * Get registry URL for pulling images
     * For Docker Hub, automatically selects best mirror
     * For other registries, returns the original URL
     */
    private fun getRegistryUrl(originalRegistry: String): String {
        return when {
            // Docker Hub - use best available mirror
            originalRegistry == "registry-1.docker.io"
                    || originalRegistry.contains("docker.io") -> {
                registryManager.bestServer.value?.metadata?.value?.url
                    ?: AppContext.DEFAULT_REGISTRY
            }
            // Other registries - use as-is
            originalRegistry.startsWith("http") -> {
                originalRegistry
            }

            else -> {
                "https://$originalRegistry"
            }
        }
    }

    /**
     * Get manifest by digest
     */
    private suspend fun getManifestByDigest(
        imageRef: ImageReference,
        digest: String
    ): Result<ImageManifestV2> = runCatching {
        val registryUrl = getRegistryUrl(imageRef.registry)
        val authToken = authenticate(
            imageRef.repository,
            registryUrl
        ).getOrThrow()
        val response = client.get("$registryUrl/v2/${imageRef.repository}/manifests/$digest") {
            if (authToken.isNotEmpty()) {
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
     * Get manifest for an image (handles both manifest list and image manifest)
     */
    suspend fun getManifest(
        imageRef: ImageReference
    ): Result<ImageManifestV2> = runCatching {
        val registryUrl = getRegistryUrl(imageRef.registry)
        val authToken = authenticate(
            imageRef.repository,
            registryUrl
        ).getOrThrow()
        // First try to get the manifest list
        val manifestListResponse = client.get(
            "$registryUrl/v2/${imageRef.repository}/manifests/${imageRef.tag}"
        ) {
            // Only add Authorization header if we have a token
            if (authToken.isNotEmpty()) {
                header(HttpHeaders.Authorization, "Bearer $authToken")
            }
            header(
                HttpHeaders.Accept,
                listOf(
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
                val manifestList = json.decodeFromString<ManifestListResponse>(bodyText)
                val platformManifest = manifestList.manifests?.find { manifest ->
                    manifest.platform?.architecture == AppContext.ARCHITECTURE &&
                            manifest.platform.os == AppContext.DEFAULT_OS
                } ?: manifestList.manifests?.firstOrNull()
                ?: throw NoSuchElementException("No suitable manifest found for $AppContext.ARCHITECTURE")

                // Get the specific manifest
                getManifestByDigest(
                    imageRef,
                    platformManifest.digest
                ).getOrThrow()
            }

            else -> {
                // Direct manifest
                json.decodeFromString<ImageManifestV2>(bodyText)
            }
        }
    }

    /**
     * Get image configuration
     */
    suspend fun getImageConfig(
        imageRef: ImageReference,
        configDigest: String
    ): Result<ImageConfigResponse> = runCatching {
        val registryUrl = getRegistryUrl(imageRef.registry)
        val authToken = authenticate(
            imageRef.repository,
            registryUrl
        ).getOrThrow()
        val response = client.get("$registryUrl/v2/${imageRef.repository}/blobs/$configDigest") {
            if (authToken.isNotEmpty()) {
                header(HttpHeaders.Authorization, "Bearer $authToken")
            }
        }
        // Some registries (like DaoCloud) don't set ContentType header,
        // so manually parse JSON from body text
        json.decodeFromString(response.bodyAsText())
    }

    /**
     * Download a layer blob
     */
    suspend fun downloadLayer(
        imageRef: ImageReference,
        layer: LayerEntity,
        destFile: File,
        onProgress: suspend (DownloadProgress) -> Unit = { }
    ): Result<Unit> {
        return runCatching {
            val registryUrl = getRegistryUrl(imageRef.registry)
            val authToken = authenticate(imageRef.repository, registryUrl).getOrThrow()
            Timber.d("Starting layer download: ${layer.id.take(16)}, size: ${layer.size}")
            client.prepareGet("$registryUrl/v2/${imageRef.repository}/blobs/sha256:${layer.id}") {
                if (authToken.isNotEmpty()) {
                    header(HttpHeaders.Authorization, "Bearer $authToken")
                }
                timeout {
                    requestTimeoutMillis = AppContext.DOWNLOAD_TIMEOUT
                }
            }.execute { response ->
                Timber.d("Layer download response status: ${response.status}")
                val contentLength = response.contentLength() ?: layer.size
                var downloaded = 0L
                destFile.parentFile?.mkdirs()
                FileOutputStream(destFile).use { fos ->
                    val channel = response.bodyAsChannel()
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (!channel.isClosedForRead) {
                        val bytesRead = channel.readAvailable(buffer)
                        if (bytesRead > 0) {
                            fos.write(buffer, 0, bytesRead)
                            downloaded += bytesRead
                            onProgress(DownloadProgress(downloaded, contentLength))
                            if (downloaded % (512 * 1024) == 0L || downloaded == contentLength) {
                                Timber.d("Download progress: $downloaded/$contentLength bytes")
                            }
                        }
                    }
                }
                Timber.i("Layer download completed: ${layer.id.take(16)}, downloaded: $downloaded bytes")
            }
        }.onFailure { e ->
            Timber.e(e, "Layer download failed: ${layer.id.take(16)}")
        }
    }
}