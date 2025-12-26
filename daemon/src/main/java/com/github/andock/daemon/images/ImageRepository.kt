package com.github.andock.daemon.images

import com.github.andock.daemon.app.AppContext
import com.github.andock.daemon.client.ImageReference
import com.github.andock.daemon.client.RegistryClient
import com.github.andock.daemon.client.model.ImageConfig
import com.github.andock.daemon.database.dao.ImageDao
import com.github.andock.daemon.database.dao.LayerDao
import com.github.andock.daemon.database.model.ImageEntity
import com.github.andock.daemon.database.model.LayerEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Repository for image management
 */
@Singleton
class ImageRepository @Inject constructor(
    private val imageDao: ImageDao,
    private val layerDao: LayerDao,
    private val registryApi: RegistryClient,
    private val appContext: AppContext
) {

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

            try {
                Timber.d("About to check database for existing layer")
                // Check if layer already exists
                existingLayer = layerDao.getLayerById(layerDigest)
                Timber.d("Database check complete: $existingLayer")

                layerFile =
                    File(appContext.layersDir, "${layerDigest.removePrefix("sha256:")}.tar.gz")
                Timber.d("File paths created")
            } catch (e: Exception) {
                Timber.e(e, "Error checking layer existence")
                throw e
            }

            if (existingLayer?.downloaded == true && layerFile.exists()) {
                // Layer already exists, increment reference
                Timber.i("Layer ${layerDigest.take(16)} already exists, skipping download")
//                layerDao.incrementRefCount(layerDigest)
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
                downloaded = false,
//                refCount = 0
            )

            val downloadResult =
                registryApi.downloadLayer(imageRef, layer, layerFile) { downloaded, total ->
                    // We can't emit from inside the callback, progress is tracked externally
                }

            Timber.d("downloadLayer returned: success=${downloadResult.isSuccess}, failure=${downloadResult.isFailure}")
            downloadResult.getOrThrow()

            // Save layer info (keep compressed file only)
            layerDao.insertLayer(
                LayerEntity(
                    id = layerDigest,
                    size = layerDescriptor.size,
                    mediaType = layerDescriptor.mediaType,
                    downloaded = true,
//                    refCount = 1
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

        // Calculate total size (compressed files)
        var totalSize = 0L
        layerIds.forEach { digest ->
            val layerFile = File(appContext.layersDir, "${digest.removePrefix("sha256:")}.tar.gz")
            totalSize += layerFile.length()
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
            id = manifest.config.digest,
            architecture = configResponse.architecture ?: AppContext.ARCHITECTURE,
            os = configResponse.os ?: AppContext.DEFAULT_OS,
            layerIds = layerIds,
            size = 0,
            created = 0,
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
//                layerDao.decrementRefCount(digest)

                // Delete layer if no longer referenced
                val layer = layerDao.getLayerById(digest)
                if (layer != null
//                    && layer.refCount <= 1
                ) {
                    val layerFile =
                        File(appContext.layersDir, "${digest.removePrefix("sha256:")}.tar.gz")
                    layerFile.delete()
                    layerDao.deleteUnreferencedLayer(digest)
                }
            }

            imageDao.deleteImageById(imageId)
        }
    }


}