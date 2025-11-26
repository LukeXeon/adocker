package com.github.adocker.daemon.images

import com.github.adocker.daemon.config.AppConfig
import com.github.adocker.daemon.database.dao.ImageDao
import com.github.adocker.daemon.database.dao.LayerDao
import com.github.adocker.daemon.registry.model.ImageConfig
import com.github.adocker.daemon.database.model.ImageEntity
import com.github.adocker.daemon.database.model.LayerEntity
import com.github.adocker.daemon.registry.DockerRegistryApi
import com.github.adocker.daemon.utils.copyDirectory
import com.github.adocker.daemon.utils.deleteRecursively
import com.github.adocker.daemon.utils.extractTar
import com.github.adocker.daemon.utils.extractTarGz
import com.github.adocker.daemon.utils.getDirectorySize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Repository for image management
 */
@Singleton
class ImageRepository @Inject constructor(
    private val imageDao: ImageDao,
    private val layerDao: LayerDao,
    private val registryApi: DockerRegistryApi,
    private val appConfig: AppConfig
) {
    /**
     * Get all local images
     */
    fun getAllImages(): Flow<List<ImageEntity>> {
        return imageDao.getAllImages()
    }

    /**
     * Get image by ID
     */
    suspend fun getImageById(id: String): ImageEntity? {
        return imageDao.getImageById(id)
    }

    /**
     * Search Docker Hub
     */
    suspend fun searchImages(query: String) = registryApi.search(query)

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
        val configResponse =
            registryApi.getImageConfig(imageRef, manifest.config.digest).getOrThrow()
        emit(PullProgress("config", 1, 1, PullStatus.DONE))

        val layers = manifest.layers
        val layerIds = mutableListOf<String>()

        // Download and extract each layer
        for (layerDescriptor in layers) {
            val layerDigest = layerDescriptor.digest
            emit(PullProgress(layerDigest, 0, layerDescriptor.size, PullStatus.WAITING))
            Timber.d("Processing layer ${layerDigest.take(16)}, size: ${layerDescriptor.size}")

            val existingLayer: LayerEntity?
            val layerFile: File
            val extractedDir: File

            try {
                Timber.d("About to check database for existing layer")
                // Check if layer already exists
                existingLayer = layerDao.getLayerByDigest(layerDigest)
                Timber.d("Database check complete: $existingLayer")

                layerFile =
                    File(appConfig.layersDir, "${layerDigest.removePrefix("sha256:")}.tar.gz")
                extractedDir = File(appConfig.layersDir, layerDigest.removePrefix("sha256:"))
                Timber.d("File paths created")

                Timber.d("Existing layer: $existingLayer, extracted dir exists: ${extractedDir.exists()}")
            } catch (e: Exception) {
                Timber.e(e, "Error checking layer existence")
                throw e
            }

            if (existingLayer?.extracted == true && extractedDir.exists()) {
                // Layer already exists, increment reference
                Timber.i("Layer ${layerDigest.take(16)} already exists, skipping download")
                layerDao.incrementRefCount(layerDigest)
                layerIds.add(layerDigest)
                emit(
                    PullProgress(
                        layerDigest,
                        layerDescriptor.size,
                        layerDescriptor.size,
                        PullStatus.DONE
                    )
                )
                continue
            }

            // Download layer
            Timber.d("Calling downloadLayer for ${layerDigest.take(16)}")
            val layer = LayerEntity(
                layerDigest,
                layerDescriptor.size,
                layerDescriptor.mediaType,
                false,
                false
            )

            val downloadResult =
                registryApi.downloadLayer(imageRef, layer, layerFile) { downloaded, total ->
                    // We can't emit from inside the callback, progress is tracked externally
                }

            Timber.d("downloadLayer returned: success=${downloadResult.isSuccess}, failure=${downloadResult.isFailure}")
            downloadResult.getOrThrow()

            emit(
                PullProgress(
                    layerDigest,
                    layerDescriptor.size,
                    layerDescriptor.size,
                    PullStatus.EXTRACTING
                )
            )

            // Extract layer
            FileInputStream(layerFile).use { fis ->
                extractTarGz(fis, extractedDir).getOrThrow()
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
            emit(
                PullProgress(
                    layerDigest,
                    layerDescriptor.size,
                    layerDescriptor.size,
                    PullStatus.DONE
                )
            )
        }

        // Calculate total size
        var totalSize = 0L
        layerIds.forEach { digest ->
            val layerDir = File(appConfig.layersDir, digest.removePrefix("sha256:"))
            totalSize += getDirectorySize(layerDir)
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
        val imageEntity = ImageEntity(
            repository = imageRef.repository,
            tag = imageRef.tag,
            digest = manifest.config.digest,
            architecture = configResponse.architecture ?: AppConfig.Companion.ARCHITECTURE,
            os = configResponse.os ?: AppConfig.Companion.DEFAULT_OS,
            size = totalSize,
            layerIds = layerIds,
            config = imageConfig
        )

        imageDao.insertImage(imageEntity)

        // Emit final completion status
        emit(PullProgress("image", totalSize, totalSize, PullStatus.DONE))

    }.buffer(capacity = 64).flowOn(Dispatchers.IO)

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
                    val layerDir = File(appConfig.layersDir, digest.removePrefix("sha256:"))
                    deleteRecursively(layerDir)
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
        val imageRef = ImageReference.Companion.parse(imageName)
        return registryApi.getTags(imageRef)
    }

    /**
     * Import image from tar file
     */
    suspend fun importImage(tarFile: File, repository: String, tag: String): Result<ImageEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                val imageId = UUID.randomUUID().toString()
                val extractDir = File(appConfig.layersDir, imageId)

                FileInputStream(tarFile).use { fis ->
                    extractTar(fis, extractDir).getOrThrow()
                }

                val size = getDirectorySize(extractDir)

                val imageEntity = ImageEntity(
                    id = imageId,
                    repository = repository,
                    tag = tag,
                    digest = "sha256:$imageId",
                    architecture = AppConfig.Companion.ARCHITECTURE,
                    os = AppConfig.Companion.DEFAULT_OS,
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

                imageDao.insertImage(imageEntity)
                imageEntity
            }
        }

    /**
     * Export image to tar file
     */
    suspend fun exportImage(imageId: String, destFile: File): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val image = imageDao.getImageById(imageId)
                    ?: throw IllegalArgumentException("Image not found: $imageId")

                // Create tar from layers
                val tempDir = File(appConfig.tmpDir, "export_$imageId")
                tempDir.mkdirs()

                // Copy all layers
                image.layerIds.forEach { digest ->
                    val layerDir = File(appConfig.layersDir, digest.removePrefix("sha256:"))
                    if (layerDir.exists()) {
                        copyDirectory(layerDir, File(tempDir, "layer"))
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

}