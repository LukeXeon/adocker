package com.adocker.runner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adocker.runner.core.config.Config
import com.adocker.runner.core.config.RegistrySettings
import com.adocker.runner.data.local.AppDatabase
import com.adocker.runner.data.remote.api.DockerRegistryApi
import com.adocker.runner.data.repository.ContainerRepository
import com.adocker.runner.data.repository.ImageRepository
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
            val registryApi = DockerRegistryApi()
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
                    android.util.Log.w("ImagePullAndRunTest",
                        "PRoot not available. Binary exists: ${prootFile.exists()}, " +
                        "Executable: ${prootFile.canExecute()}, " +
                        "Path: ${prootFile.absolutePath}")
                }
            } catch (e: Exception) {
                // PRoot not available, skip PRoot-dependent tests
                android.util.Log.e("ImagePullAndRunTest", "Failed to initialize PRoot: ${e.message}", e)
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

        android.util.Log.d("ImagePullAndRunTest", "Native lib dir: ${nativeLibDir.absolutePath}")
        android.util.Log.d("ImagePullAndRunTest", "PRoot binary: ${prootBinary.absolutePath}, exists: ${prootBinary.exists()}")

        // List all files in native lib dir for debugging
        nativeLibDir.listFiles()?.forEach { file ->
            android.util.Log.d("ImagePullAndRunTest", "  - ${file.name} (${file.length()} bytes)")
        }

        if (!prootBinary.exists()) {
            throw IllegalStateException("PRoot binary not found in native lib dir: ${prootBinary.absolutePath}")
        }

        return PRootEngine(prootBinary, nativeLibDir)
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

            // Use Docker Hub directly for testing (mirrors may not work in emulator)
            // Switch to official Docker Hub for this test
            RegistrySettings.setMirror(
                com.adocker.runner.core.config.RegistryMirror(
                    name = "Docker Hub",
                    url = "https://registry-1.docker.io",
                    authUrl = "https://auth.docker.io",
                    isDefault = false,
                    isBuiltIn = true
                )
            )

            // Test with Alpine Linux (small image)
            val testImage = "alpine:latest"

            // Step 1: Pull image (skip on network errors)
            var pullCompleted = false
            try {
                withTimeout(180000) { // 3 minute timeout
                    imageRepository.pullImage(testImage).collect { progress ->
                        android.util.Log.d("ImagePullAndRunTest", "Pull progress: ${progress.status} - ${progress.layerDigest}")
                        when (progress.status) {
                            com.adocker.runner.domain.model.PullStatus.DONE -> {
                                pullCompleted = true
                            }
                            com.adocker.runner.domain.model.PullStatus.ERROR -> {
                                // Don't fail on network errors, just skip
                                android.util.Log.w("ImagePullAndRunTest", "Pull error: ${progress.layerDigest}")
                            }
                            else -> { /* Continue */ }
                        }
                    }
                }
            } catch (e: java.net.UnknownHostException) {
                assumeTrue("Skipping test - network unavailable: ${e.message}", false)
                return@runBlocking
            } catch (e: java.net.SocketTimeoutException) {
                assumeTrue("Skipping test - network timeout: ${e.message}", false)
                return@runBlocking
            } catch (e: Exception) {
                if (e.message?.contains("resolve host") == true ||
                    e.message?.contains("Network") == true ||
                    e.cause is java.net.UnknownHostException) {
                    assumeTrue("Skipping test - network error: ${e.message}", false)
                    return@runBlocking
                }
                throw e
            }

            assumeTrue("Skipping test - image pull failed (possibly network issue)", pullCompleted)

            // Step 2: Verify image exists
            val images = imageRepository.getAllImages().first()
            val pulledImage = images.find { it.fullName.contains("alpine") }
            assertNotNull("Alpine image should be in database", pulledImage)

            // Step 3: Create container
            val containerConfig = com.adocker.runner.domain.model.ContainerConfig(
                cmd = listOf("/bin/sh", "-c", "echo 'Hello from ADocker'"),
                workingDir = "/",
                env = emptyMap()
            )
            val createResult = containerRepository.createContainer(
                imageId = pulledImage!!.id,
                name = "test-alpine-${System.currentTimeMillis()}",
                config = containerConfig
            )
            assertTrue("Container should be created successfully", createResult.isSuccess)
            val container = createResult.getOrThrow()
            assertNotNull("Container should not be null", container)
            assertEquals("Container should be stopped initially", com.adocker.runner.domain.model.ContainerStatus.STOPPED, container.status)

            // Step 4: Start container
            withTimeout(30000) { // 30 second timeout
                executor.startContainer(container.id)
            }

            // Step 5: Verify container ran
            val containerAfterStart = containerRepository.getContainerById(container.id)
            assertNotNull("Container should still exist", containerAfterStart)
            // Container may be running or exited after running the command
            assertTrue(
                "Container should be running or exited",
                containerAfterStart?.status == com.adocker.runner.domain.model.ContainerStatus.RUNNING ||
                containerAfterStart?.status == com.adocker.runner.domain.model.ContainerStatus.EXITED
            )
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
                android.util.Log.e("ImagePullAndRunTest", "Cleanup failed: ${e.message}", e)
            }

            // Close database
            try {
                database.close()
            } catch (e: Exception) {
                android.util.Log.e("ImagePullAndRunTest", "Database close failed: ${e.message}", e)
            }
        }
    }
}
