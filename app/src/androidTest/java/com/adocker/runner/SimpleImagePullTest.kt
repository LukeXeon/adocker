package com.adocker.runner

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adocker.runner.core.config.Config
import com.adocker.runner.core.config.RegistrySettings
import com.adocker.runner.data.local.AppDatabase
import com.adocker.runner.data.remote.api.DockerRegistryApi
import com.adocker.runner.data.repository.ImageRepository
import com.adocker.runner.domain.model.PullStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Simple test to verify image pull without container execution
 */
@RunWith(AndroidJUnit4::class)
class SimpleImagePullTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var registryApi: DockerRegistryApi
    private lateinit var imageRepository: ImageRepository

    @Before
    fun setup() {
        runBlocking {
            context = ApplicationProvider.getApplicationContext()
            Config.init(context)
            RegistrySettings.init(context)

            database = AppDatabase.getInstance(context)
            registryApi = DockerRegistryApi()
            imageRepository = ImageRepository(
                database.imageDao(),
                database.layerDao(),
                registryApi
            )
        }
    }

    @Test
    fun testPullAlpineImageFromChinaMirror() {
        runBlocking {
            val currentMirror = RegistrySettings.getCurrentMirror()
            Log.i("SimpleImagePullTest", "========================================")
            Log.i("SimpleImagePullTest", "Testing Alpine Image Pull")
            Log.i("SimpleImagePullTest", "Registry: ${currentMirror.name} (${currentMirror.url})")
            Log.i("SimpleImagePullTest", "========================================")

            val testImage = "alpine:latest"
            var pullSuccessful = false
            var totalLayers = 0
            var completedLayers = 0

            try {
                withTimeout(300000) { // 5 minutes
                    imageRepository.pullImage(testImage).collect { progress ->
                        Log.d("SimpleImagePullTest", "[${progress.status}] ${progress.layerDigest} - ${progress.downloaded}/${progress.total} bytes")

                        when (progress.status) {
                            PullStatus.DOWNLOADING -> {
                                if (progress.layerDigest != "manifest" && progress.layerDigest != "config") {
                                    totalLayers = maxOf(totalLayers, 1)
                                }
                            }
                            PullStatus.DONE -> {
                                if (progress.layerDigest == "image") {
                                    pullSuccessful = true
                                    Log.i("SimpleImagePullTest", "✅ Image pull COMPLETED!")
                                } else if (progress.layerDigest != "manifest" && progress.layerDigest != "config") {
                                    completedLayers++
                                    Log.i("SimpleImagePullTest", "✅ Layer completed: ${progress.layerDigest.take(12)} ($completedLayers layers done)")
                                }
                            }
                            PullStatus.ERROR -> {
                                Log.e("SimpleImagePullTest", "❌ Error: ${progress.layerDigest}")
                            }
                            else -> {}
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SimpleImagePullTest", "Pull failed with exception", e)
                // Check if it's a network error
                if (e.message?.contains("resolve host") == true ||
                    e.message?.contains("Network") == true ||
                    e is java.net.UnknownHostException) {
                    assumeTrue("Skipping - network unavailable: ${e.message}", false)
                    return@runBlocking
                }
                throw e
            }

            Log.i("SimpleImagePullTest", "========================================")
            Log.i("SimpleImagePullTest", "PULL RESULT")
            Log.i("SimpleImagePullTest", "Success: $pullSuccessful")
            Log.i("SimpleImagePullTest", "Layers: $completedLayers/$totalLayers")
            Log.i("SimpleImagePullTest", "========================================")

            // Verify image in database
            val images = imageRepository.getAllImages().first()
            val alpineImage = images.find { it.repository.contains("alpine") }

            assertNotNull("Alpine image should be in database", alpineImage)
            assertTrue("Image should have layers", alpineImage!!.layerIds.isNotEmpty())
            assertTrue("Image should have size > 0", alpineImage.size > 0)

            Log.i("SimpleImagePullTest", "✅ DATABASE VERIFICATION PASSED")
            Log.i("SimpleImagePullTest", "Image: ${alpineImage.fullName}")
            Log.i("SimpleImagePullTest", "Size: ${alpineImage.size} bytes")
            Log.i("SimpleImagePullTest", "Layers: ${alpineImage.layerIds.size}")

            // Verify layers on disk
            alpineImage.layerIds.forEach { digest ->
                val layerDir = File(Config.layersDir, digest.removePrefix("sha256:"))
                assertTrue("Layer should exist: ${layerDir.absolutePath}", layerDir.exists())
                assertTrue("Layer should not be empty", layerDir.listFiles()?.isNotEmpty() == true)
                Log.i("SimpleImagePullTest", "✅ Layer verified: ${digest.take(12)}")
            }

            Log.i("SimpleImagePullTest", "========================================")
            Log.i("SimpleImagePullTest", "🎉 ALL TESTS PASSED!")
            Log.i("SimpleImagePullTest", "========================================")
        }
    }

    @After
    fun cleanup() {
        runBlocking {
            try {
                // Don't close database - let it be closed naturally
                // Closing it early causes job cancellation
                // database.close()
                Log.d("SimpleImagePullTest", "Cleanup completed (database left open)")
            } catch (e: Exception) {
                Log.e("SimpleImagePullTest", "Cleanup error: ${e.message}")
            }
        }
    }
}
