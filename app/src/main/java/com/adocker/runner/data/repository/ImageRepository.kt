package com.adocker.runner.data.repository

import com.adocker.runner.core.config.Config
import com.adocker.runner.core.utils.FileUtils
import com.adocker.runner.data.local.dao.ImageDao
import com.adocker.runner.data.local.dao.LayerDao
import com.adocker.runner.data.local.entity.ImageEntity
import com.adocker.runner.data.local.entity.LayerEntity
import com.adocker.runner.data.remote.api.DockerRegistryApi
import com.adocker.runner.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream

/**
 * Repository for image management - equivalent to udocker's LocalRepository
 */
class ImageRepository(
    private val imageDao: ImageDao,
    private val layerDao: LayerDao,
    private val registryApi: DockerRegistryApi
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    /**
     * Get all local images
     */
    fun getAllImages(): Flow<List<LocalImage>> {
        return imageDao.getAllImages().map { entities ->
            entities.map { it.toLocalImage() }
        }
    }

    /**
     * Get image by ID
     */
    suspend fun getImageById(id: String): LocalImage? {
        return imageDao.getImageById(id)?.toLocalImage()
    }

    /**
     * Search Docker Hub
     */
    suspend fun searchImages(query: String): Result<List<SearchResult>> {
        return registryApi.search(query).map { results ->
            results.map { dto ->
                SearchResult(
                    name = dto.name,
                    description = dto.description ?: "",
                    starCount = dto.starCount,
                    isOfficial = dto.isOfficial,
                    isAutomated = dto.isAutomated
                )
            }
        }
    }

    /**
     * Pull an image from registry
     */
    fun pullImage(imageName: String): Flow<PullProgress> = flow {
        val imageRef = ImageReference.parse(imageName)

        // Get manifest
        emit(PullProgress("manifest", 0, 1, PullStatus.DOWNLOADING))
        val manifest = registryApi.getManifest(imageRef).getOrThrow()
        emit(PullProgress("manifest", 1, 1, PullStatus.DONE))

        // Get image config
        emit(PullProgress("config", 0, 1, PullStatus.DOWNLOADING))
        val configResponse = registryApi.getImageConfig(imageRef, manifest.config.digest).getOrThrow()
        emit(PullProgress("config", 1, 1, PullStatus.DONE))

        val layers = manifest.layers
        val layerIds = mutableListOf<String>()

        // Download and extract each layer
        for ((index, layerDescriptor) in layers.withIndex()) {
            val layerDigest = layerDescriptor.digest
            val shortDigest = layerDigest.removePrefix("sha256:").take(12)

            emit(PullProgress(layerDigest, 0, layerDescriptor.size, PullStatus.WAITING))

            // Check if layer already exists
            val existingLayer = layerDao.getLayerByDigest(layerDigest)
            val layerFile = File(Config.layersDir, "${layerDigest.removePrefix("sha256:")}.tar.gz")
            val extractedDir = File(Config.layersDir, layerDigest.removePrefix("sha256:"))

            if (existingLayer?.extracted == true && extractedDir.exists()) {
                // Layer already exists, increment reference
                layerDao.incrementRefCount(layerDigest)
                layerIds.add(layerDigest)
                emit(PullProgress(layerDigest, layerDescriptor.size, layerDescriptor.size, PullStatus.DONE))
                continue
            }

            // Download layer
            val layer = Layer(layerDigest, layerDescriptor.size, layerDescriptor.mediaType)

            registryApi.downloadLayer(imageRef, layer, layerFile) { downloaded, total ->
                // We can't emit from inside the callback, progress is tracked externally
            }.getOrThrow()

            emit(PullProgress(layerDigest, layerDescriptor.size, layerDescriptor.size, PullStatus.EXTRACTING))

            // Extract layer
            FileInputStream(layerFile).use { fis ->
                FileUtils.extractTarGz(fis, extractedDir).getOrThrow()
            }

            // Clean up compressed file
            layerFile.delete()

            // Save layer info
            layerDao.insertLayer(
                LayerEntity(
                    digest = layerDigest,
                    size = layerDescriptor.size,
                    mediaType = layerDescriptor.mediaType,
                    downloaded = true,
                    extracted = true,
                    refCount = 1
                )
            )

            layerIds.add(layerDigest)
            emit(PullProgress(layerDigest, layerDescriptor.size, layerDescriptor.size, PullStatus.DONE))
        }

        // Calculate total size
        var totalSize = 0L
        layerIds.forEach { digest ->
            val layerDir = File(Config.layersDir, digest.removePrefix("sha256:"))
            totalSize += FileUtils.getDirectorySize(layerDir)
        }

        // Create image config
        val imageConfig = ImageConfig(
            cmd = configResponse.config?.cmd,
            entrypoint = configResponse.config?.entrypoint,
            env = configResponse.config?.env,
            workingDir = configResponse.config?.workingDir,
            user = configResponse.config?.user
        )

        // Save image to database
        val localImage = LocalImage(
            repository = imageRef.repository,
            tag = imageRef.tag,
            digest = manifest.config.digest,
            architecture = configResponse.architecture ?: Config.getArchitecture(),
            os = configResponse.os ?: Config.DEFAULT_OS,
            size = totalSize,
            layerIds = layerIds,
            config = imageConfig
        )

        imageDao.insertImage(localImage.toEntity())

    }.flowOn(Dispatchers.IO)

    /**
     * Delete a local image
     */
    suspend fun deleteImage(imageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val image = imageDao.getImageById(imageId)
                ?: throw IllegalArgumentException("Image not found: $imageId")

            // Decrement layer references
            image.layerIds.forEach { digest ->
                layerDao.decrementRefCount(digest)

                // Delete layer if no longer referenced
                val layer = layerDao.getLayerByDigest(digest)
                if (layer != null && layer.refCount <= 1) {
                    val layerDir = File(Config.layersDir, digest.removePrefix("sha256:"))
                    FileUtils.deleteRecursively(layerDir)
                    layerDao.deleteUnreferencedLayer(digest)
                }
            }

            imageDao.deleteImageById(imageId)
        }
    }

    /**
     * Get tags for an image
     */
    suspend fun getTags(imageName: String): Result<List<String>> {
        val imageRef = ImageReference.parse(imageName)
        return registryApi.getTags(imageRef)
    }

    /**
     * Import image from tar file
     */
    suspend fun importImage(tarFile: File, repository: String, tag: String): Result<LocalImage> =
        withContext(Dispatchers.IO) {
            runCatching {
                val imageId = java.util.UUID.randomUUID().toString()
                val extractDir = File(Config.layersDir, imageId)

                FileInputStream(tarFile).use { fis ->
                    FileUtils.extractTar(fis, extractDir).getOrThrow()
                }

                val size = FileUtils.getDirectorySize(extractDir)

                val localImage = LocalImage(
                    id = imageId,
                    repository = repository,
                    tag = tag,
                    digest = "sha256:$imageId",
                    architecture = Config.getArchitecture(),
                    os = Config.DEFAULT_OS,
                    size = size,
                    layerIds = listOf("sha256:$imageId")
                )

                // Save layer
                layerDao.insertLayer(
                    LayerEntity(
                        digest = "sha256:$imageId",
                        size = size,
                        mediaType = "application/vnd.docker.image.rootfs.diff.tar",
                        downloaded = true,
                        extracted = true,
                        refCount = 1
                    )
                )

                imageDao.insertImage(localImage.toEntity())
                localImage
            }
        }

    /**
     * Export image to tar file
     */
    suspend fun exportImage(imageId: String, destFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val image = imageDao.getImageById(imageId)
                ?: throw IllegalArgumentException("Image not found: $imageId")

            // Create tar from layers
            val tempDir = File(Config.tmpDir, "export_$imageId")
            tempDir.mkdirs()

            // Copy all layers
            image.layerIds.forEach { digest ->
                val layerDir = File(Config.layersDir, digest.removePrefix("sha256:"))
                if (layerDir.exists()) {
                    FileUtils.copyDirectory(layerDir, File(tempDir, "layer"))
                }
            }

            // Create tar (simplified - real implementation would use TarArchiveOutputStream)
            // For now, just copy the rootfs
            val rootfsDir = File(tempDir, "layer")
            if (rootfsDir.exists()) {
                rootfsDir.copyRecursively(destFile.parentFile!!, overwrite = true)
            }

            tempDir.deleteRecursively()
            Unit
        }
    }

    private fun ImageEntity.toLocalImage(): LocalImage {
        return LocalImage(
            id = id,
            repository = repository,
            tag = tag,
            digest = digest,
            architecture = architecture,
            os = os,
            created = created,
            size = size,
            layerIds = layerIds,
            config = configJson?.let { json.decodeFromString(it) }
        )
    }

    private fun LocalImage.toEntity(): ImageEntity {
        return ImageEntity(
            id = id,
            repository = repository,
            tag = tag,
            digest = digest,
            architecture = architecture,
            os = os,
            created = created,
            size = size,
            layerIds = layerIds,
            configJson = config?.let { json.encodeToString(it) }
        )
    }
}
