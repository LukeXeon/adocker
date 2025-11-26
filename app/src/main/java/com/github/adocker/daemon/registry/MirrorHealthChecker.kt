package com.github.adocker.daemon.registry

import com.github.adocker.daemon.database.dao.MirrorDao
import com.github.adocker.daemon.database.model.MirrorEntity
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

@Singleton
class MirrorHealthChecker @Inject constructor(
    private val mirrorDao: MirrorDao,
    private val client: HttpClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _isChecking = MutableStateFlow(false)
    val isChecking: Flow<Boolean> = _isChecking.asStateFlow()

    companion object {
        private const val HEALTH_CHECK_INTERVAL = 5 * 60 * 1000L // 5 minutes
        private const val PING_TIMEOUT = 5000L // 5 seconds
        private const val UNHEALTHY_THRESHOLD = 3 // Mark as unhealthy after 3 consecutive failures
    }

    private val failureCount = mutableMapOf<String, Int>()

    init {
        // Start periodic health checks
        scope.launch {
            while (true) {
                delay(HEALTH_CHECK_INTERVAL)
                checkAllMirrors()
            }
        }
    }

    /**
     * Check health of all mirrors
     */
    suspend fun checkAllMirrors() {
        if (_isChecking.value) {
            Timber.d("Health check already in progress, skipping")
            return
        }

        _isChecking.value = true
        try {
            val mirrors = mutableListOf<MirrorEntity>()
            mirrorDao.getAllMirrors().collect { list ->
                mirrors.addAll(list)
            }

            Timber.d("Starting health check for ${mirrors.size} mirrors")

            mirrors.forEach { mirror ->
                checkMirror(mirror)
            }

            Timber.d("Health check completed")
        } catch (e: Exception) {
            Timber.e(e, "Error during health check")
        } finally {
            _isChecking.value = false
        }
    }

    /**
     * Check health of a single mirror
     */
    suspend fun checkMirror(mirror: MirrorEntity): Boolean {
        return try {
            Timber.d("Checking health of mirror: ${mirror.name} (${mirror.url})")

            val latency = measureTimeMillis {
                val response: HttpResponse = client.get("${mirror.url}/v2/")

                // Accept both OK and Unauthorized (401) as healthy
                // 401 means the registry is responding but requires auth
                if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.Unauthorized) {
                    throw Exception("Unexpected status code: ${response.status}")
                }
            }

            // Mark as healthy
            mirrorDao.updateMirrorHealth(
                url = mirror.url,
                isHealthy = true,
                latencyMs = latency,
                lastChecked = System.currentTimeMillis()
            )

            failureCount[mirror.url] = 0

            Timber.i("Mirror ${mirror.name} is healthy (latency: ${latency}ms)")
            true
        } catch (e: Exception) {
            Timber.w(e, "Mirror ${mirror.name} health check failed: ${e.message}")

            // Increment failure count
            val failures = failureCount.getOrDefault(mirror.url, 0) + 1
            failureCount[mirror.url] = failures

            // Only mark as unhealthy after threshold
            if (failures >= UNHEALTHY_THRESHOLD) {
                mirrorDao.updateMirrorHealth(
                    url = mirror.url,
                    isHealthy = false,
                    latencyMs = -1,
                    lastChecked = System.currentTimeMillis()
                )
                Timber.w("Mirror ${mirror.name} marked as unhealthy after $failures failures")
            }

            false
        }
    }

    /**
     * Force an immediate health check
     */
    fun checkNow() {
        scope.launch {
            checkAllMirrors()
        }
    }

    /**
     * Reset failure count for a mirror (e.g., when user manually adds/enables it)
     */
    fun resetFailureCount(mirrorUrl: String) {
        failureCount[mirrorUrl] = 0
    }
}
