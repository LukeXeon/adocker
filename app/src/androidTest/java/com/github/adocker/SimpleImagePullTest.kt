package com.github.adocker

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.github.adocker.daemon.config.AppConfig
import com.github.adocker.daemon.registry.RegistryRepository
import com.github.adocker.daemon.images.ImageRepository
import com.github.adocker.daemon.images.PullStatus
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import javax.inject.Inject

/**
 * Simple test to verify image pull without container execution
 */
@HiltAndroidTest
class SimpleImagePullTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var appConfig: AppConfig

    @Inject
    lateinit var registrySettings: RegistryRepository

    @Inject
    lateinit var imageRepository: ImageRepository

    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        runBlocking {
            context = ApplicationProvider.getApplicationContext()
        }
    }

    @Test
    fun testPullAlpineImageFromChinaMirror() {
        runBlocking {
            val currentMirror = registrySettings.getCurrentMirror()
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
                val layerDir = File(appConfig.layersDir, digest.removePrefix("sha256:"))
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
                Log.d("SimpleImagePullTest", "Cleanup completed (database left open)")
            } catch (e: Exception) {
                Log.e("SimpleImagePullTest", "Cleanup error: ${e.message}")
            }
        }
    }
}
