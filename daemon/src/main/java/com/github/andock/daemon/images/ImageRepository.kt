package com.github.andock.daemon.images

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.github.andock.daemon.app.AppArchitecture
import com.github.andock.daemon.database.dao.AuthTokenDao
import com.github.andock.daemon.database.dao.RegistryDao
import com.github.andock.daemon.database.model.AuthTokenEntity
import com.github.andock.daemon.database.model.LayerEntity
import com.github.andock.daemon.images.models.AuthTokenResponse
import com.github.andock.daemon.images.models.ImageConfigResponse
import com.github.andock.daemon.images.models.ImageManifestV2
import com.github.andock.daemon.images.models.ManifestListResponse
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
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
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Singleton

class ImageRepository @AssistedInject constructor(
    @Assisted
    private val registryUrl: String,
    private val client: HttpClient,
    private val registryDao: RegistryDao,
    private val authTokenDao: AuthTokenDao,
    private val json: Json,
    private val factory: ImageTagPagingSource.Factory,
) {
    companion object {
        private const val DOWNLOAD_TIMEOUT = 300000L
        private const val N = 100
        private val realmRegex = Regex("realm=\"([^\"]+)\"")
        private val serviceRegex = Regex("service=\"([^\"]+)\"")
    }

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
    suspend fun authenticate(repository: String): Result<String> = runCatching {
        Timber.d("Authenticating for repository: $repository, registry: $registryUrl")
        try {
            // Check if this specific registry URL has a Bearer Token configured
            val bearerToken = registryDao.getTokenByUrl(registryUrl)
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

            val token = authTokenDao.findByUrl(authUrl)
            if (token != null) {
                if (System.currentTimeMillis() < token.expiry) {
                    return@runCatching token.token
                } else {
                    authTokenDao.deleteExpired()
                }
            }

            Timber.d("Requesting token from: $authUrl")
            val tokenResponse = client.get(authUrl).body<AuthTokenResponse>()
            val authToken = tokenResponse.token ?: tokenResponse.accessToken ?: ""
            authTokenDao.insert(
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
     * Get manifest by digest
     */
    suspend fun getManifestByDigest(
        repository: String,
        digest: String
    ): Result<ImageManifestV2> = runCatching {
        val authToken = authenticate(repository).getOrThrow()
        val response = client.get("$registryUrl/v2/${repository}/manifests/$digest") {
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
        if (response.status == HttpStatusCode.Unauthorized) {
            authTokenDao.delete(authToken)
        }
        response.body()
    }

    /**
     * Get manifest for an image (handles both manifest list and image manifest)
     */
    suspend fun getManifest(
        repository: String,
        tag: String
    ): Result<ImageManifestV2> = runCatching {
        val authToken = authenticate(repository).getOrThrow()
        // First try to get the manifest list
        val response = client.get(
            "$registryUrl/v2/${repository}/manifests/${tag}"
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
        if (response.status == HttpStatusCode.Unauthorized) {
            authTokenDao.delete(authToken)
        }
        val contentType = response.contentType()?.toString() ?: ""
        val bodyText = response.bodyAsText()
        Timber.d("Manifest response - ContentType: $contentType")
        Timber.d("Manifest response - Body: ${bodyText.take(500)}")
        when {
            contentType.contains("manifest.list") || contentType.contains("image.index") -> {
                // It's a manifest list, find the right architecture
                val manifestList = json.decodeFromString<ManifestListResponse>(bodyText)
                val platformManifest = manifestList.manifests?.find { manifest ->
                    manifest.platform?.architecture == AppArchitecture.DEFAULT &&
                            manifest.platform.os == AppArchitecture.OS
                } ?: manifestList.manifests?.firstOrNull()
                ?: throw NoSuchElementException("No suitable manifest found for ${AppArchitecture.OS}:${AppArchitecture.DEFAULT}")

                // Get the specific manifest
                getManifestByDigest(
                    repository,
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
        repository: String,
        configDigest: String
    ): Result<ImageConfigResponse> = runCatching {
        val authToken = authenticate(repository).getOrThrow()
        val response = client.get("$registryUrl/v2/${repository}/blobs/$configDigest") {
            if (authToken.isNotEmpty()) {
                header(HttpHeaders.Authorization, "Bearer $authToken")
            }
        }
        if (response.status == HttpStatusCode.Unauthorized) {
            authTokenDao.delete(authToken)
        }
        // Some registries (like DaoCloud) don't set ContentType header,
        // so manually parse JSON from body text
        val body = json.decodeFromString<ImageConfigResponse>(response.bodyAsText())
        if (body.architecture != AppArchitecture.DEFAULT || body.os != AppArchitecture.OS) {
            throw NoSuchElementException("No config found for ${AppArchitecture.OS}:${AppArchitecture.DEFAULT}")
        }
        return@runCatching body
    }

    /**
     * Download a layer blob
     */
    suspend fun downloadLayer(
        repository: String,
        layer: LayerEntity,
        destFile: File,
        onProgress: suspend (DownloadProgress) -> Unit = { }
    ): Result<Unit> {
        return runCatching {
            val authToken = authenticate(repository).getOrThrow()
            Timber.d("Starting layer download: ${layer.id.take(16)}, size: ${layer.size}")
            client.prepareGet("$registryUrl/v2/${repository}/blobs/sha256:${layer.id}") {
                if (authToken.isNotEmpty()) {
                    header(HttpHeaders.Authorization, "Bearer $authToken")
                }
                timeout {
                    requestTimeoutMillis = DOWNLOAD_TIMEOUT
                }
            }.execute { response ->
                if (response.status == HttpStatusCode.Unauthorized) {
                    authTokenDao.delete(authToken)
                }
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

    fun tags(repository: String): Flow<PagingData<String>> {
        return Pager(
            config = PagingConfig(
                pageSize = N,
                enablePlaceholders = false,
                initialLoadSize = N,
            ),
            initialKey = ImageTagPagingKey(
                registry = registryUrl,
                repository = repository,
                pageSize = N,
                last = null
            ),
            pagingSourceFactory = factory
        ).flow
    }


    @Singleton
    @AssistedFactory
    interface Factory : (String) -> ImageRepository {
        override fun invoke(@Assisted registryUrl: String): ImageRepository
    }
}