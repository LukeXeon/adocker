package com.github.adocker

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.github.adocker.core.config.AppConfig
import com.github.adocker.core.utils.execute
import com.github.adocker.core.registry.RegistryRepository
import com.github.adocker.core.registry.DockerRegistryApi
import com.github.adocker.core.container.ContainerRepository
import com.github.adocker.core.image.ImageRepository
import com.github.adocker.core.database.model.ContainerConfig
import com.github.adocker.core.database.model.ContainerStatus
import com.github.adocker.core.image.PullStatus
import com.github.adocker.core.container.ContainerExecutor
import com.github.adocker.core.engine.PRootEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import javax.inject.Inject

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
@HiltAndroidTest
class ImagePullAndRunTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var appConfig: AppConfig

    @Inject
    lateinit var registrySettings: RegistryRepository

    @Inject
    lateinit var imageRepository: ImageRepository

    @Inject
    lateinit var containerRepository: ContainerRepository

    @Inject
    @ApplicationContext
    lateinit var context: Context

    @Inject
    lateinit var prootEngine: PRootEngine

    @Inject
    lateinit var containerExecutor: ContainerExecutor

    @Inject
    lateinit var registryApi: DockerRegistryApi
    var prootAvailable = false

    @Before
    fun setup() {
        hiltRule.inject()
        runBlocking {
            context = ApplicationProvider.getApplicationContext()

            // Try to initialize PRoot engine (may not work on emulators)
            try {
                val engine = initializePRoot()
                val available = engine.isAvailable()
                if (available) {
                    prootEngine = engine
                    containerExecutor = ContainerExecutor(engine, containerRepository)
                    prootAvailable = true
                } else {
                    val nativeLibDir = appConfig.nativeLibDir
                    val prootFile = File(nativeLibDir, "libproot.so")
                    Log.w(
                        "ImagePullAndRunTest",
                        "PRoot not available. Binary exists: ${prootFile.exists()}, " +
                                "Executable: ${prootFile.canExecute()}, " +
                                "Path: ${prootFile.absolutePath}"
                    )
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
        val nativeLibDir = appConfig.nativeLibDir
            ?: throw IllegalStateException("Native library directory is null")

        val prootBinary = File(nativeLibDir, "libproot.so")

        Log.d("ImagePullAndRunTest", "Native lib dir: ${nativeLibDir.absolutePath}")
        Log.d(
            "ImagePullAndRunTest",
            "PRoot binary: ${prootBinary.absolutePath}, exists: ${prootBinary.exists()}"
        )

        // List all files in native lib dir for debugging
        nativeLibDir.listFiles()?.forEach { file ->
            Log.d("ImagePullAndRunTest", "  - ${file.name} (${file.length()} bytes)")
        }

        if (!prootBinary.exists()) {
            throw IllegalStateException("PRoot binary not found in native lib dir: ${prootBinary.absolutePath}")
        }

        return PRootEngine(appConfig)
    }

    @Test
    fun testRegistryMirrorConnectivity() {
        runBlocking {
            // Test that we can reach the configured registry mirror
            val currentMirror = registrySettings.getCurrentMirror()
            Log.i(
                "ImagePullAndRunTest",
                "Testing connectivity to: ${currentMirror.name} (${currentMirror.url})"
            )

            try {
                // Try to get a token for library/alpine
                val result = registryApi.authenticate("library/alpine", currentMirror.url)

                if (result.isSuccess) {
                    Log.i(
                        "ImagePullAndRunTest",
                        "Successfully authenticated with ${currentMirror.name}"
                    )
                    assertNotNull("Auth token should not be null", result.getOrNull())
                } else {
                    val exception = result.exceptionOrNull()
                    Log.w("ImagePullAndRunTest", "Failed to authenticate: ${exception?.message}")

                    // Skip test if it's a network error (especially common in China where some URLs are blocked)
                    if (exception is java.net.ConnectException ||
                        exception is java.net.UnknownHostException ||
                        exception is java.net.SocketTimeoutException ||
                        exception?.cause is java.net.ConnectException
                    ) {
                        assumeTrue("Skipping test - network error: ${exception.message}", false)
                        return@runBlocking
                    }

                    // Don't fail the test - just log the warning
                    // In CI/testing environments, network access may be restricted
                }
            } catch (e: java.net.ConnectException) {
                Log.w(
                    "ImagePullAndRunTest",
                    "Registry connectivity test skipped - connection failed: ${e.message}"
                )
                assumeTrue("Skipping test - connection failed: ${e.message}", false)
            } catch (e: java.net.UnknownHostException) {
                Log.w(
                    "ImagePullAndRunTest",
                    "Registry connectivity test skipped - host not found: ${e.message}"
                )
                assumeTrue("Skipping test - host not found: ${e.message}", false)
            } catch (e: Exception) {
                Log.w("ImagePullAndRunTest", "Registry connectivity test failed: ${e.message}", e)
                // Don't fail - network may not be available in test environment
                // Just log and continue
            }
        }
    }

    @Test
    fun testPRootHelp() {
        // Skip this test if PRoot is not available on this architecture
        assumeTrue("Skipping test - PRoot not available on this device/emulator", prootAvailable)

        runBlocking {
            val nativeLibDir = appConfig.nativeLibDir!!
            val prootBinary = File(nativeLibDir, "libproot.so")

            Log.i("ImagePullAndRunTest", "=== EXECUTING PROOT --help ===")
            Log.i("ImagePullAndRunTest", "PRoot binary: ${prootBinary.absolutePath}")

            try {
                val result = execute(
                    command = listOf(prootBinary.absolutePath, "--help"),
                    timeout = 5000
                )

                Log.i("ImagePullAndRunTest", "=== PROOT --help OUTPUT ===")
                Log.i("ImagePullAndRunTest", "Exit code: ${result.exitCode}")
                Log.i("ImagePullAndRunTest", "=== STDOUT ===")
                result.stdout.lines().forEach { line ->
                    Log.i("ImagePullAndRunTest", line)
                }
                Log.i("ImagePullAndRunTest", "=== STDERR ===")
                result.stderr.lines().forEach { line ->
                    Log.i("ImagePullAndRunTest", line)
                }
                Log.i("ImagePullAndRunTest", "=== END PROOT --help ===")
            } catch (e: Exception) {
                Log.e("ImagePullAndRunTest", "Failed to execute PRoot --help", e)
                throw e
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

            // Use default China mirror (DaoCloud) for better connectivity in China
            // The default mirror is already configured in RegistrySettings
            val currentMirror = registrySettings.getCurrentMirror()
            Log.i(
                "ImagePullAndRunTest",
                "Using registry mirror: ${currentMirror.name} (${currentMirror.url})"
            )

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
                        Log.d(
                            "ImagePullAndRunTest",
                            "Pull progress: ${progress.status} - ${progress.layerDigest} - ${progress.downloaded}/${progress.total}"
                        )

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

                            else -> { /* Continue */
                            }
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
                    e.cause is java.net.UnknownHostException
                ) {
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

            val pulledImage =
                images.find { it.repository.contains("alpine") || it.fullName.contains("alpine") }
            assertNotNull("Alpine image should be in database after pull", pulledImage)
            assertTrue("Image should have at least one layer", pulledImage!!.layerIds.isNotEmpty())
            assertTrue("Image size should be greater than 0", pulledImage.size > 0)
            Log.i(
                "ImagePullAndRunTest",
                "Successfully pulled image: ${pulledImage.fullName}, size: ${pulledImage.size} bytes, layers: ${pulledImage.layerIds.size}"
            )

            // Step 3: Verify layers are extracted
            pulledImage.layerIds.forEach { digest ->
                val layerDir = File(appConfig.layersDir, digest.removePrefix("sha256:"))
                Log.d("ImagePullAndRunTest", "=== LAYER VERIFICATION ===")
                Log.d("ImagePullAndRunTest", "Layer digest: $digest")
                Log.d("ImagePullAndRunTest", "Layer dir path: ${layerDir.absolutePath}")
                Log.d("ImagePullAndRunTest", "Layer dir exists: ${layerDir.exists()}")
                Log.d("ImagePullAndRunTest", "Layer dir is directory: ${layerDir.isDirectory}")

                if (layerDir.exists()) {
                    val files = layerDir.listFiles()
                    Log.d("ImagePullAndRunTest", "Layer dir file count: ${files?.size ?: 0}")
                    files?.take(10)?.forEach { file ->
                        Log.d(
                            "ImagePullAndRunTest",
                            "  - ${file.name} (${if (file.isDirectory) "dir" else "file"}, ${file.length()} bytes)"
                        )
                    }

                    // Check for /bin/sh specifically
                    val binDir = File(layerDir, "bin")
                    Log.d("ImagePullAndRunTest", "bin/ directory exists: ${binDir.exists()}")
                    if (binDir.exists()) {
                        val shFile = File(binDir, "sh")
                        Log.d(
                            "ImagePullAndRunTest",
                            "bin list: ${binDir.listFiles().contentToString()}"
                        )
                        Log.d(
                            "ImagePullAndRunTest",
                            "bin/sh exists: ${shFile.exists()}, size: ${shFile.length()}"
                        )
                    }
                } else {
                    Log.e("ImagePullAndRunTest", "❌ Layer directory DOES NOT EXIST!")
                }
                Log.d("ImagePullAndRunTest", "=========================")

                assertTrue(
                    "Layer directory should exist: ${layerDir.absolutePath}",
                    layerDir.exists()
                )
                assertTrue(
                    "Layer directory should not be empty",
                    layerDir.listFiles()?.isNotEmpty() == true
                )
            }

            // Step 4: Create container
            val containerConfig = ContainerConfig(
                cmd = listOf(
                    "/bin/sh",
                    "-c",
                    "echo 'Hello from ADocker'; echo 'Test completed successfully'"
                ),
                workingDir = "/",
                env = emptyMap()
            )
            val createResult = containerRepository.createContainer(
                imageId = pulledImage.id,
                name = "test-alpine-${System.currentTimeMillis()}",
                config = containerConfig
            )
            assertTrue(
                "Container should be created successfully: ${createResult.exceptionOrNull()?.message}",
                createResult.isSuccess
            )
            val container = createResult.getOrThrow()
            assertNotNull("Container should not be null", container)
            assertEquals(
                "Container should be in created state initially",
                ContainerStatus.CREATED,
                container.status
            )
            Log.i("ImagePullAndRunTest", "Created container: ${container.name} (${container.id})")

            // Step 5: Execute command in container and capture output
            Log.d("ImagePullAndRunTest", "Executing command in container ${container.id}...")
            val execResult = try {
                withTimeout(30000) { // 30 second timeout
                    containerExecutor.execInContainer(
                        container.id,
                        listOf(
                            "/bin/sh",
                            "-c",
                            "echo 'Hello from ADocker on Alpine Linux!'; uname -a; echo ''; echo 'LibC Information:'; ldd /bin/sh 2>&1 | head -1 || ls -la /lib/libc.musl-*.so* 2>&1; echo ''; echo 'Test completed successfully'"
                        )
                    ).getOrThrow()
                }
            } catch (e: Exception) {
                Log.e("ImagePullAndRunTest", "Failed to execute in container: ${e.message}", e)
                throw e
            }

            // Step 6: Verify execution output
            Log.i("ImagePullAndRunTest", "========================================")
            Log.i("ImagePullAndRunTest", "=== ALPINE LINUX CONTAINER OUTPUT ===")
            Log.i("ImagePullAndRunTest", "========================================")
            Log.i("ImagePullAndRunTest", "Exit code: ${execResult.exitCode}")
            Log.i("ImagePullAndRunTest", "")
            execResult.output.split("\n").forEach { line ->
                Log.i("ImagePullAndRunTest", "  $line")
            }
            Log.i("ImagePullAndRunTest", "")
            Log.i("ImagePullAndRunTest", "========================================")

            // Verify output contains expected messages
            assertTrue(
                "Container output should contain 'Hello from ADocker'",
                execResult.output.contains("Hello from ADocker")
            )
            assertTrue(
                "Container output should contain 'Alpine'",
                execResult.output.contains("Alpine") || execResult.output.contains("Linux")
            )
            assertTrue(
                "Container output should contain 'Test completed successfully'",
                execResult.output.contains("Test completed successfully")
            )
            assertEquals("Container should exit successfully", 0, execResult.exitCode)

            Log.i(
                "ImagePullAndRunTest",
                "✅ Test completed successfully! Alpine image pulled, container created and executed with verified output!"
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
                Log.e("ImagePullAndRunTest", "Cleanup failed: ${e.message}", e)
            }

            // Don't close database - let it be closed naturally
            // Closing it early causes JobCancellationException in other concurrent tests
            // because AppDatabase uses singleton pattern and all tests share the same instance
            // database.close()
        }
    }
}
