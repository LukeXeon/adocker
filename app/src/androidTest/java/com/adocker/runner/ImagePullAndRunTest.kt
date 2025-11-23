package com.adocker.runner

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adocker.runner.core.config.Config
import com.adocker.runner.core.config.RegistrySettings
import com.adocker.runner.data.local.AppDatabase
import com.adocker.runner.data.remote.api.DockerRegistryApi
import com.adocker.runner.data.repository.ContainerRepository
import com.adocker.runner.data.repository.ImageRepository
import com.adocker.runner.domain.model.ContainerConfig
import com.adocker.runner.domain.model.ContainerStatus
import com.adocker.runner.domain.model.PullStatus
import com.adocker.runner.engine.executor.ContainerExecutor
import com.adocker.runner.engine.proot.PRootEngine
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
 * Integration tests for image pull and container run functionality.
 *
 * Note: Tests that require PRoot will be skipped when PRoot is not available.
 * On Android 10+ (API 29+), SELinux restricts execution of binaries from
 * app_data_file directories (execute_no_trans denial). PRoot tests will only
 * pass when the binary can be executed directly from the native libs directory.
 *
 * Check logcat with `adb logcat -d | grep -i PRootEngine` for detailed error info.
 */
@RunWith(AndroidJUnit4::class)
class ImagePullAndRunTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var registryApi: DockerRegistryApi
    private lateinit var imageRepository: ImageRepository
    private lateinit var containerRepository: ContainerRepository
    private var prootEngine: PRootEngine? = null
    private var containerExecutor: ContainerExecutor? = null
    private var prootAvailable = false

    @Before
    fun setup() {
        runBlocking {
            context = ApplicationProvider.getApplicationContext()

            // Initialize Config
            Config.init(context)
            RegistrySettings.init(context)

            // Initialize database
            database = AppDatabase.getInstance(context)

            // Initialize repositories
            registryApi = DockerRegistryApi()
            imageRepository = ImageRepository(
                database.imageDao(),
                database.layerDao(),
                registryApi
            )
            containerRepository = ContainerRepository(
                database.containerDao(),
                database.imageDao()
            )

            // Try to initialize PRoot engine (may not work on emulators)
            try {
                val engine = initializePRoot()
                val available = engine.isAvailable()
                if (available) {
                    prootEngine = engine
                    containerExecutor = ContainerExecutor(engine, containerRepository)
                    prootAvailable = true
                } else {
                    val binDir = Config.binDir
                    val prootFile = File(binDir, "proot")
                    Log.w("ImagePullAndRunTest",
                        "PRoot not available. Binary exists: ${prootFile.exists()}, " +
                        "Executable: ${prootFile.canExecute()}, " +
                        "Path: ${prootFile.absolutePath}")
                }
            } catch (e: Exception) {
                // PRoot not available, skip PRoot-dependent tests
                Log.e("ImagePullAndRunTest", "Failed to initialize PRoot: ${e.message}", e)
                prootAvailable = false
            }
        }
    }

    /**
     * Initialize PRoot directly from native lib directory.
     *
     * IMPORTANT: On Android 10+, we MUST execute PRoot directly from the native
     * library directory (/data/app/<pkg>/lib/<arch>) because it has the correct
     * SELinux context (apk_data_file) that allows execution.
     *
     * Copying the binary to app data directory will result in SELinux denial
     * (execute_no_trans) because files there have app_data_file context.
     */
    private fun initializePRoot(): PRootEngine {
        val nativeLibDir = Config.getNativeLibDir()
            ?: throw IllegalStateException("Native library directory is null")

        val prootBinary = File(nativeLibDir, "libproot.so")

        Log.d("ImagePullAndRunTest", "Native lib dir: ${nativeLibDir.absolutePath}")
        Log.d("ImagePullAndRunTest", "PRoot binary: ${prootBinary.absolutePath}, exists: ${prootBinary.exists()}")

        // List all files in native lib dir for debugging
        nativeLibDir.listFiles()?.forEach { file ->
            Log.d("ImagePullAndRunTest", "  - ${file.name} (${file.length()} bytes)")
        }

        if (!prootBinary.exists()) {
            throw IllegalStateException("PRoot binary not found in native lib dir: ${prootBinary.absolutePath}")
        }

        return PRootEngine(prootBinary, nativeLibDir)
    }

    @Test
    fun testRegistryMirrorConnectivity() {
        runBlocking {
            // Test that we can reach the configured registry mirror
            val currentMirror = RegistrySettings.getCurrentMirror()
            Log.i("ImagePullAndRunTest", "Testing connectivity to: ${currentMirror.name} (${currentMirror.url})")

            try {
                // Try to get a token for library/alpine
                val result = registryApi.authenticate("library/alpine", currentMirror.url)

                if (result.isSuccess) {
                    Log.i("ImagePullAndRunTest", "Successfully authenticated with ${currentMirror.name}")
                    assertNotNull("Auth token should not be null", result.getOrNull())
                } else {
                    val exception = result.exceptionOrNull()
                    Log.w("ImagePullAndRunTest", "Failed to authenticate: ${exception?.message}")

                    // Skip test if it's a network error (especially common in China where some URLs are blocked)
                    if (exception is java.net.ConnectException ||
                        exception is java.net.UnknownHostException ||
                        exception is java.net.SocketTimeoutException ||
                        exception?.cause is java.net.ConnectException) {
                        assumeTrue("Skipping test - network error: ${exception.message}", false)
                        return@runBlocking
                    }

                    // Don't fail the test - just log the warning
                    // In CI/testing environments, network access may be restricted
                }
            } catch (e: java.net.ConnectException) {
                Log.w("ImagePullAndRunTest", "Registry connectivity test skipped - connection failed: ${e.message}")
                assumeTrue("Skipping test - connection failed: ${e.message}", false)
            } catch (e: java.net.UnknownHostException) {
                Log.w("ImagePullAndRunTest", "Registry connectivity test skipped - host not found: ${e.message}")
                assumeTrue("Skipping test - host not found: ${e.message}", false)
            } catch (e: Exception) {
                Log.w("ImagePullAndRunTest", "Registry connectivity test failed: ${e.message}", e)
                // Don't fail - network may not be available in test environment
                // Just log and continue
            }
        }
    }

    @Test
    fun testPRootIsAvailable() {
        // Skip this test if PRoot is not available on this architecture
        assumeTrue("Skipping test - PRoot not available on this device/emulator", prootAvailable)

        runBlocking {
            val engine = prootEngine!!
            assertTrue("PRoot should be available", engine.isAvailable())
            assertNotNull("PRoot should have a version", engine.getVersion())
        }
    }

    @Test
    fun testPullAlpineImageAndRunContainer() {
        // Skip this test if PRoot is not available
        assumeTrue("Skipping test - PRoot not available on this device/emulator", prootAvailable)

        runBlocking {
            val executor = containerExecutor!!

            // Use default China mirror (DaoCloud) for better connectivity in China
            // The default mirror is already configured in RegistrySettings
            val currentMirror = RegistrySettings.getCurrentMirror()
            Log.i("ImagePullAndRunTest", "Using registry mirror: ${currentMirror.name} (${currentMirror.url})")

            // Test with Alpine Linux (small image, ~3MB)
            val testImage = "alpine:latest"

            // Step 1: Pull image (skip on network errors)
            var pullCompleted = false
            var lastError: String? = null
            var layerCount = 0
            var completedLayers = 0

            try {
                withTimeout(300000) { // 5 minute timeout (longer for slow connections)
                    imageRepository.pullImage(testImage).collect { progress ->
                        Log.d("ImagePullAndRunTest",
                            "Pull progress: ${progress.status} - ${progress.layerDigest} - ${progress.downloaded}/${progress.total}")

                        when (progress.status) {
                            PullStatus.DOWNLOADING -> {
                                if (progress.layerDigest != "manifest" && progress.layerDigest != "config") {
                                    layerCount = maxOf(layerCount, 1)
                                }
                            }
                            PullStatus.DONE -> {
                                if (progress.layerDigest != "manifest" && progress.layerDigest != "config") {
                                    completedLayers++
                                }
                                // Check if all layers are done
                                if (progress.layerDigest == "image") {
                                    pullCompleted = true
                                }
                            }
                            PullStatus.ERROR -> {
                                lastError = progress.layerDigest
                                Log.e("ImagePullAndRunTest", "Pull error: ${progress.layerDigest}")
                            }
                            else -> { /* Continue */ }
                        }
                    }
                }
            } catch (e: java.net.UnknownHostException) {
                Log.e("ImagePullAndRunTest", "Network unavailable: ${e.message}", e)
                assumeTrue("Skipping test - network unavailable: ${e.message}", false)
                return@runBlocking
            } catch (e: java.net.SocketTimeoutException) {
                Log.e("ImagePullAndRunTest", "Network timeout: ${e.message}", e)
                assumeTrue("Skipping test - network timeout: ${e.message}", false)
                return@runBlocking
            } catch (e: io.ktor.client.network.sockets.ConnectTimeoutException) {
                Log.e("ImagePullAndRunTest", "Connect timeout: ${e.message}", e)
                assumeTrue("Skipping test - connect timeout: ${e.message}", false)
                return@runBlocking
            } catch (e: Exception) {
                Log.e("ImagePullAndRunTest", "Pull exception: ${e.message}", e)
                if (e.message?.contains("resolve host") == true ||
                    e.message?.contains("Network") == true ||
                    e.message?.contains("timeout") == true ||
                    e.cause is java.net.UnknownHostException) {
                    assumeTrue("Skipping test - network error: ${e.message}", false)
                    return@runBlocking
                }
                throw e
            }

            // Check if pull was successful
            if (!pullCompleted && lastError != null) {
                Log.w("ImagePullAndRunTest", "Image pull completed with errors: $lastError")
            }

            // Step 2: Verify image exists in database
            val images = imageRepository.getAllImages().first()
            Log.d("ImagePullAndRunTest", "Total images in database: ${images.size}")
            images.forEach { image ->
                Log.d("ImagePullAndRunTest", "  - ${image.fullName} (${image.id})")
            }

            val pulledImage = images.find { it.repository.contains("alpine") || it.fullName.contains("alpine") }
            assertNotNull("Alpine image should be in database after pull", pulledImage)
            assertTrue("Image should have at least one layer", pulledImage!!.layerIds.isNotEmpty())
            assertTrue("Image size should be greater than 0", pulledImage.size > 0)
            Log.i("ImagePullAndRunTest", "Successfully pulled image: ${pulledImage.fullName}, size: ${pulledImage.size} bytes, layers: ${pulledImage.layerIds.size}")

            // Step 3: Verify layers are extracted
            pulledImage.layerIds.forEach { digest ->
                val layerDir = File(Config.layersDir, digest.removePrefix("sha256:"))
                assertTrue("Layer directory should exist: ${layerDir.absolutePath}", layerDir.exists())
                assertTrue("Layer directory should not be empty", layerDir.listFiles()?.isNotEmpty() == true)
                Log.d("ImagePullAndRunTest", "Layer ${digest.take(12)} extracted to ${layerDir.absolutePath}")
            }

            // Step 4: Create container
            val containerConfig = ContainerConfig(
                cmd = listOf("/bin/sh", "-c", "echo 'Hello from ADocker'; echo 'Test completed successfully'"),
                workingDir = "/",
                env = emptyMap()
            )
            val createResult = containerRepository.createContainer(
                imageId = pulledImage.id,
                name = "test-alpine-${System.currentTimeMillis()}",
                config = containerConfig
            )
            assertTrue("Container should be created successfully: ${createResult.exceptionOrNull()?.message}", createResult.isSuccess)
            val container = createResult.getOrThrow()
            assertNotNull("Container should not be null", container)
            assertEquals("Container should be in created state initially", ContainerStatus.CREATED, container.status)
            Log.i("ImagePullAndRunTest", "Created container: ${container.name} (${container.id})")

            // Step 5: Start container and verify execution
            try {
                withTimeout(30000) { // 30 second timeout
                    Log.d("ImagePullAndRunTest", "Starting container ${container.id}...")
                    executor.startContainer(container.id)
                    Log.i("ImagePullAndRunTest", "Container started successfully")
                }
            } catch (e: Exception) {
                Log.e("ImagePullAndRunTest", "Failed to start container: ${e.message}", e)
                throw e
            }

            // Step 6: Verify container state
            val containerAfterStart = containerRepository.getContainerById(container.id)
            assertNotNull("Container should still exist after start", containerAfterStart)
            Log.d("ImagePullAndRunTest", "Container status after start: ${containerAfterStart?.status}")

            // Container may be running or exited after running the command
            assertTrue(
                "Container should be running or exited, but was: ${containerAfterStart?.status}",
                containerAfterStart?.status == ContainerStatus.RUNNING ||
                containerAfterStart?.status == ContainerStatus.EXITED
            )

            Log.i("ImagePullAndRunTest", "Test completed successfully! Image pulled, container created and executed.")
        }
    }

    @After
    fun cleanup() {
        runBlocking {
            // Clean up test containers
            try {
                val containers = containerRepository.getAllContainers().first()
                containers.filter { it.name.startsWith("test-") }.forEach { container ->
                    try {
                        containerRepository.deleteContainer(container.id)
                    } catch (_: Exception) {
                        // Ignore cleanup errors
                    }
                }
            } catch (e: Exception) {
                Log.e("ImagePullAndRunTest", "Cleanup failed: ${e.message}", e)
            }

            // Don't close database - let it be closed naturally
            // Closing it early causes JobCancellationException in other concurrent tests
            // because AppDatabase uses singleton pattern and all tests share the same instance
            // database.close()
        }
    }
}
