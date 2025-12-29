package com.github.andock.daemon

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.andock.daemon.engine.PRootEngine
import com.github.andock.daemon.images.ImageRepository
import com.github.andock.daemon.images.model.ContainerConfig
import com.github.andock.daemon.containers.ContainerManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber
import java.net.UnknownHostException
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Instrumented test for Alpine Linux image pull and container execution.
 *
 * This test verifies the complete container lifecycle:
 * 1. Pulling Alpine Linux image from Docker Hub
 * 2. Creating a container with custom command to check libc version
 * 3. Executing the container and capturing output
 * 4. Verifying Alpine's musl libc information is present in output
 * 5. Proper cleanup of resources
 *
 * Uses official Android coroutines testing best practices:
 * - runTest for coroutine test support with virtual time
 * - StandardTestDispatcher for deterministic test execution
 * - Proper Hilt integration for dependency injection
 *
 * References:
 * - https://developer.android.com/kotlin/coroutines/test
 * - https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AlpineImagePullAndRunTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var imageRepository: ImageRepository

    @Inject
    lateinit var containerManager: ContainerManager

    @Inject
    lateinit var prootEngine: PRootEngine

    private var createdContainerId: String? = null
    private var pulledImageId: String? = null

    @Before
    fun setup() {
        hiltRule.inject()

        // Initialize Timber for test logging
        if (Timber.treeCount == 0) {
            Timber.plant(object : Timber.DebugTree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    println("[$tag] $message")
                    t?.printStackTrace()
                }
            })
        }

        Timber.d("=== Test Setup ===")
        Timber.d("PRoot available: ${prootEngine.isAvailable}")
        Timber.d("PRoot version: ${prootEngine.version}")
        Timber.d("==================")
    }

    @After
    fun tearDown() = runTest(timeout = 2.minutes) {
        Timber.d("=== Test Teardown ===")

        // Stop and remove container if created
        createdContainerId?.let { containerId ->
            try {
                Timber.d("Stopping container: $containerId")
                containerManager.stopContainer(containerId)
                    .onSuccess { Timber.d("Container stopped successfully") }
                    .onFailure { Timber.w("Failed to stop container (may already be stopped): ${it.message}") }

                delay(500) // Give it time to stop

                Timber.d("Deleting container: $containerId")
                containerManager.deleteContainer(containerId)
                    .onSuccess { Timber.d("Container deleted successfully") }
                    .onFailure { Timber.e("Failed to delete container: ${it.message}") }
            } catch (e: Exception) {
                Timber.e("Error during container cleanup: ${e.message}", e)
            }
        }

        // Remove image if pulled
        pulledImageId?.let { imageId ->
            try {
                Timber.d("Deleting image: $imageId")
                imageRepository.deleteImage(imageId)
                    .onSuccess { Timber.d("Image deleted successfully") }
                    .onFailure { Timber.e("Failed to delete image: ${it.message}") }
            } catch (e: Exception) {
                Timber.e("Error during image cleanup: ${e.message}", e)
            }
        }

        Timber.d("Teardown completed")
        Timber.d("=====================")
    }

    @Test
    fun testAlpineImagePullAndLibcVersionCheck() = runTest(timeout = 5.minutes) {
        // Skip test if PRoot is not available
        assumeTrue(
            "Skipping test - PRoot not available on this architecture",
            prootEngine.isAvailable
        )

        val imageName = "alpine:latest"
        Timber.d("=== Alpine Linux Image Pull and Run Test ===")
        Timber.d("Target image: $imageName")
        Timber.d("============================================")

        // ========================================
        // STEP 1: Pull Alpine image
        // ========================================
        Timber.d("")
        Timber.d("STEP 1: Pulling Alpine image...")
        var imageId: String? = null

        try {
            imageRepository.pullImage(imageName)
                .collect { progress ->
                    when (progress.status) {
                        PullStatus.DOWNLOADING -> {
                            val percent = if (progress.total > 0) {
                                (progress.downloaded * 100 / progress.total)
                            } else 0
                            Timber.d("  Downloading ${progress.layerDigest.take(12)}: $percent% (${progress.downloaded}/${progress.total} bytes)")
                        }

                        PullStatus.DONE -> {
                            if (progress.layerDigest == "image") {
                                Timber.d("  Image pull completed successfully!")
                            }
                        }

                        PullStatus.ERROR -> {
                            Timber.e("  Image pull failed for layer ${progress.layerDigest}")
                            throw AssertionError("Failed to pull image layer: ${progress.layerDigest}")
                        }

                        else -> {
                            Timber.d("  ${progress.status}: ${progress.layerDigest.take(12)}")
                        }
                    }
                }
        } catch (e: UnknownHostException) {
            assumeTrue("Skipping test - network unavailable: ${e.message}", false)
        } catch (e: Exception) {
            Timber.e("Unexpected error during image pull: ${e.message}", e)
            throw e
        }

        // Get the pulled image from database
        val images = imageRepository.getAllImages().first()
        val pulledImage = images.find { it.repository == "library/alpine" && it.tag == "latest" }
        assertNotNull("Image not found in database after pull", pulledImage)
        imageId = pulledImage!!.id
        pulledImageId = imageId
        Timber.d("Image pulled successfully with ID: $imageId")

        // ========================================
        // STEP 2: Create container with libc check command
        // ========================================
        Timber.d("")
        Timber.d("STEP 2: Creating container with libc version check command...")

        // Alpine uses musl libc - check version with: ldd --version
        // The command outputs musl libc information to stderr, so we redirect it
        val libcCheckCommand = listOf("/bin/sh", "-c", "ldd --version 2>&1")

        val containerConfig = ContainerConfig(
            cmd = libcCheckCommand,
            workingDir = "/",
            user = "root",
            env = mapOf(
                "TERM" to "xterm-256color",
                "PATH" to "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
            )
        )

        val createResult = containerManager.createContainer(
            imageId = imageId,
            name = "alpine-test-${System.currentTimeMillis()}",
            config = containerConfig
        )

        assertTrue(
            "Failed to create container: ${createResult.exceptionOrNull()?.message}",
            createResult.isSuccess
        )
        val container = createResult.getOrThrow()
        createdContainerId = container.id
        Timber.d("Container created successfully")
        Timber.d("  ID: ${container.id}")
        Timber.d("  Name: ${container.name}")

        // ========================================
        // STEP 3: Start container
        // ========================================
        Timber.d("")
        Timber.d("STEP 3: Starting container...")
        val startResult = containerManager.startContainer(container.id)
        assertTrue(
            "Failed to start container: ${startResult.exceptionOrNull()?.message}",
            startResult.isSuccess
        )
        Timber.d("Container started successfully")

        // Give container a moment to start
        delay(1000)

        // ========================================
        // STEP 4: Get running container instance
        // ========================================
        Timber.d("")
        Timber.d("STEP 4: Getting running container instance...")
        val runningContainer = containerManager.getRunningContainer(container.id)

        assertNotNull("Running container not found: ${container.id}", runningContainer)
        Timber.d("Running container found")
        Timber.d("  Active: ${runningContainer!!.job.isActive}")

        // ========================================
        // STEP 5: Read and verify output
        // ========================================
        Timber.d("")
        Timber.d("STEP 5: Reading container output...")

        val output = readContainerOutput(runningContainer, timeoutSeconds = 30)

        Timber.d("Waiting for auto-cleanup to complete...")
        delay(1000) // Give monitor thread time to cleanup

        Timber.d("Container output received (${output.length} characters):")
        Timber.d("--- OUTPUT START ---")
        output.lines().forEach { line ->
            Timber.d(line)
        }
        Timber.d("--- OUTPUT END ---")

        // ========================================
        // STEP 6: Verify libc information
        // ========================================
        Timber.d("")
        Timber.d("STEP 6: Verifying Alpine musl libc information...")

        // Alpine Linux uses musl libc
        // Expected output should contain "musl libc" text
        val hasMuslLibc = output.contains("musl", ignoreCase = true)
        assertTrue(
            "Expected musl libc information in output, but 'musl' keyword not found.\nActual output:\n$output",
            hasMuslLibc
        )

        Timber.d("SUCCESS: musl libc information verified!")
        Timber.d("")
        Timber.d("=== Test Completed Successfully ===")
    }

    /**
     * Read container output with timeout.
     *
     * This method uses the RunningContainer's exposed input reader to read stdout.
     * It waits for the container process to complete or timeout, then reads all available output.
     *
     * @param container The running container to read from
     * @param timeoutSeconds Maximum time to wait for output
     * @return The complete output from the container
     */
    private suspend fun readContainerOutput(
        container: RunningContainer,
        timeoutSeconds: Int
    ): String {
        val output = StringBuilder()

        // Wait for process to complete or timeout
        val exitCode = withTimeoutOrNull(timeoutSeconds.seconds) {
            var attempts = 0
            while (container.job.isActive && attempts < timeoutSeconds * 10) {
                delay(100)
                attempts++
            }

            // Process has exited
            if (!container.job.isActive) {
                0 // Assume successful exit
            } else {
                null
            }
        }

        Timber.d("Container process exit code: $exitCode")

        // Read output using the exposed input reader
        try {
            container.stdout.useLines { lines ->
                lines.forEach { line ->
                    output.appendLine(line)
                }
            }
        } catch (e: Exception) {
            Timber.w("Error reading container output: ${e.message}")
        }

        // Note: stderr reading removed as mainProcess is private
        // All output should come through the exposed input reader

        return output.toString()
    }
}
