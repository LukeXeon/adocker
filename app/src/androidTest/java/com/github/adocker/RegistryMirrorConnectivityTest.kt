package com.github.adocker

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.github.adocker.core.config.AppConfig
import com.github.adocker.data.repository.RegistryRepository
import com.github.adocker.data.local.model.MirrorEntity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

/**
 * Test to verify connectivity and availability of all built-in registry mirrors.
 * This test will check which mirrors are accessible from China and report the results.
 */
@HiltAndroidTest
class RegistryMirrorConnectivityTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var appConfig: AppConfig

    @Inject
    lateinit var registrySettings: RegistryRepository

    private lateinit var context: Context
    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 15000  // 15 second timeout
            connectTimeoutMillis = 15000
            socketTimeoutMillis = 15000
        }
    }

    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testAllMirrorsConnectivity() {
        runBlocking {
            Log.i("MirrorTest", "========================================")
            Log.i("MirrorTest", "Testing connectivity to all registry mirrors")
            Log.i("MirrorTest", "========================================")

            val results = mutableListOf<MirrorTestResult>()

            for (mirror in RegistryRepository.BUILT_IN_MIRRORS) {
                val result = testMirrorConnectivity(mirror)
                results.add(result)

                val status = if (result.isAccessible) "✓ ACCESSIBLE" else "✗ FAILED"
                Log.i("MirrorTest", "$status - ${mirror.name}")
                Log.i("MirrorTest", "  URL: ${mirror.url}")
                Log.i("MirrorTest", "  Latency: ${result.latencyMs}ms")
                if (!result.isAccessible) {
                    Log.w("MirrorTest", "  Error: ${result.error}")
                }
                Log.i("MirrorTest", "----------------------------------------")
            }

            // Print summary
            Log.i("MirrorTest", "========================================")
            Log.i("MirrorTest", "SUMMARY")
            Log.i("MirrorTest", "========================================")
            val accessible = results.filter { it.isAccessible }
            val failed = results.filter { !it.isAccessible }

            Log.i("MirrorTest", "Total mirrors tested: ${results.size}")
            Log.i("MirrorTest", "Accessible: ${accessible.size}")
            Log.i("MirrorTest", "Failed: ${failed.size}")

            if (accessible.isNotEmpty()) {
                Log.i("MirrorTest", "\nAccessible mirrors (sorted by latency):")
                accessible.sortedBy { it.latencyMs }.forEach { result ->
                    Log.i("MirrorTest", "  - ${result.mirror.name} (${result.latencyMs}ms)")
                }
            }

            if (failed.isNotEmpty()) {
                Log.w("MirrorTest", "\nFailed mirrors:")
                failed.forEach { result ->
                    Log.w("MirrorTest", "  - ${result.mirror.name}: ${result.error}")
                }
            }

            // Recommend best mirror
            val bestMirror = accessible.minByOrNull { it.latencyMs }
            if (bestMirror != null) {
                Log.i("MirrorTest", "\n*** RECOMMENDED MIRROR ***")
                Log.i("MirrorTest", "Name: ${bestMirror.mirror.name}")
                Log.i("MirrorTest", "URL: ${bestMirror.mirror.url}")
                Log.i("MirrorTest", "Latency: ${bestMirror.latencyMs}ms")
            } else {
                Log.e("MirrorTest", "\n*** NO MIRRORS ACCESSIBLE ***")
                Log.e("MirrorTest", "Network may be unavailable or all mirrors are down")
            }

            Log.i("MirrorTest", "========================================")
        }
    }

    @Test
    fun testCurrentMirrorConnectivity() {
        runBlocking {
            val currentMirror = registrySettings.getCurrentMirror()
            Log.i("MirrorTest", "Testing current configured mirror: ${currentMirror.name}")

            val result = testMirrorConnectivity(currentMirror)

            if (result.isAccessible) {
                Log.i("MirrorTest", "✓ Current mirror is accessible")
                Log.i("MirrorTest", "  Latency: ${result.latencyMs}ms")
            } else {
                Log.e("MirrorTest", "✗ Current mirror is NOT accessible")
                Log.e("MirrorTest", "  Error: ${result.error}")
                Log.w("MirrorTest", "  Consider switching to another mirror")
            }
        }
    }

    @Test
    fun testAuthenticationToMirrors() {
        runBlocking {
            Log.i("MirrorTest", "========================================")
            Log.i("MirrorTest", "Testing authentication to accessible mirrors")
            Log.i("MirrorTest", "========================================")

            for (mirror in RegistryRepository.BUILT_IN_MIRRORS) {
                // First test basic connectivity
                val connectivityResult = testMirrorConnectivity(mirror)
                if (!connectivityResult.isAccessible) {
                    Log.w("MirrorTest", "SKIP - ${mirror.name} (not accessible)")
                    continue
                }

                // Test authentication
                val authResult = testMirrorAuthentication(mirror)
                val status = if (authResult.success) "✓ AUTH OK" else "✗ AUTH FAILED"
                Log.i("MirrorTest", "$status - ${mirror.name}")
                if (!authResult.success) {
                    Log.w("MirrorTest", "  Error: ${authResult.error}")
                }
            }

            Log.i("MirrorTest", "========================================")
        }
    }

    private suspend fun testMirrorConnectivity(mirror: MirrorEntity): MirrorTestResult {
        return try {
            val startTime = System.currentTimeMillis()

            // Test basic connectivity by hitting the /v2/ endpoint
            val response = client.get("${mirror.url}/v2/")
            val latency = System.currentTimeMillis() - startTime

            when (response.status) {
                HttpStatusCode.OK,
                HttpStatusCode.Unauthorized,  // 401 is expected without auth, but shows the server is up
                HttpStatusCode.Forbidden -> {  // 403 may also indicate server is up
                    MirrorTestResult(mirror, true, latency, null)
                }

                else -> {
                    MirrorTestResult(mirror, false, latency, "HTTP ${response.status}")
                }
            }
        } catch (e: Exception) {
            val errorMsg = when (e) {
                is UnknownHostException -> "DNS resolution failed"
                is SocketTimeoutException -> "Connection timeout"
                is ConnectTimeoutException -> "Connect timeout"
                else -> e.message ?: "Unknown error"
            }
            MirrorTestResult(mirror, false, -1, errorMsg)
        }
    }

    private suspend fun testMirrorAuthentication(mirror: MirrorEntity): AuthTestResult {
        return try {
            // Try to get an auth token for library/alpine
            // Use Docker's default auth server
            val authUrl =
                "https://auth.docker.io/token?service=${AppConfig.DEFAULT_REGISTRY_SERVICE}&scope=repository:library/alpine:pull"
            val response = client.get(authUrl)

            if (response.status == HttpStatusCode.OK) {
                AuthTestResult(true, null)
            } else {
                AuthTestResult(false, "HTTP ${response.status}")
            }
        } catch (e: Exception) {
            AuthTestResult(false, e.message ?: "Unknown error")
        }
    }

    data class MirrorTestResult(
        val mirror: MirrorEntity,
        val isAccessible: Boolean,
        val latencyMs: Long,
        val error: String?
    )

    data class AuthTestResult(
        val success: Boolean,
        val error: String?
    )
}
