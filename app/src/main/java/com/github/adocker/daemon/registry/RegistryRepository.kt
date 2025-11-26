package com.github.adocker.daemon.registry

import com.github.adocker.daemon.database.dao.MirrorDao
import com.github.adocker.daemon.database.model.MirrorEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registry repository with automatic mirror selection
 *
 * Features:
 * - Docker Hub is hardcoded as fallback
 * - Automatically selects best available mirror based on health and latency
 * - Users can only add/edit/delete custom mirrors
 * - No manual mirror selection - always uses best available
 */

@Singleton
class RegistryRepository @Inject constructor(
    private val mirrorDao: MirrorDao,
    private val healthChecker: MirrorHealthChecker
) {
    init {
        // Initialize mirrors on startup using a coroutine
        CoroutineScope(Dispatchers.IO).launch {
            ensureInitialized()
        }
    }

    companion object {
        // Docker Hub as hardcoded fallback
        const val DOCKER_HUB_URL = "https://registry-1.docker.io"

        // Built-in mirrors for Docker Hub (excluding Docker Hub itself)
        val BUILT_IN_MIRRORS = listOf(
            MirrorEntity(
                name = "DaoCloud (China)",
                url = "https://docker.m.daocloud.io",
                priority = 100,
                isBuiltIn = true
            ),
            MirrorEntity(
                name = "Xuanyuan (China)",
                url = "https://docker.xuanyuan.me",
                priority = 90,
                isBuiltIn = true
            ),
            MirrorEntity(
                name = "Aliyun (China)",
                url = "https://registry.cn-hangzhou.aliyuncs.com",
                priority = 80,
                isBuiltIn = true
            ),
            MirrorEntity(
                name = "Huawei Cloud (China)",
                url = "https://mirrors.huaweicloud.com",
                priority = 70,
                isBuiltIn = true
            )
        )
    }

    /**
     * Initialize built-in mirrors on first use
     */
    private suspend fun ensureInitialized() {
        val existingMirrors = mirrorDao.getAllMirrors().first()
        if (existingMirrors.isEmpty()) {
            Timber.d("Initializing built-in mirrors")
            mirrorDao.insertMirrors(BUILT_IN_MIRRORS)

            // Trigger initial health check
            healthChecker.checkNow()
        }
    }

    /**
     * Get all mirrors (built-in + custom) for display in UI
     */
    suspend fun getAllMirrors(): List<MirrorEntity> {
        ensureInitialized()
        return mirrorDao.getAllMirrors().first()
    }

    /**
     * Get all mirrors flow for observing changes in UI
     */
    fun getAllMirrorsFlow(): Flow<List<MirrorEntity>> {
        return mirrorDao.getAllMirrors()
    }

    /**
     * Get the best available mirror based on health and latency
     * Returns Docker Hub as fallback if no mirrors are healthy
     */
    suspend fun getBestMirror(): String {
        ensureInitialized()

        val bestMirror = mirrorDao.getBestMirror()

        return if (bestMirror != null) {
            Timber.d("Using best mirror: ${bestMirror.name} (latency: ${bestMirror.latencyMs}ms)")
            bestMirror.url
        } else {
            Timber.w("No healthy mirrors available, falling back to Docker Hub")
            DOCKER_HUB_URL
        }
    }

    /**
     * Get all healthy mirrors sorted by latency (for retry logic)
     */
    suspend fun getHealthyMirrors(): List<MirrorEntity> {
        ensureInitialized()
        return mirrorDao.getHealthyMirrors()
    }

    /**
     * Get registry URL for pulling images
     * For Docker Hub, automatically selects best mirror
     * For other registries, returns the original URL
     */
    suspend fun getRegistryUrl(originalRegistry: String): String {
        return when {
            // Docker Hub - use best available mirror
            originalRegistry == "docker.io" ||
                    originalRegistry == "registry-1.docker.io" ||
                    originalRegistry.contains("docker.io") -> getBestMirror()

            // Other registries - use as-is
            originalRegistry.startsWith("http") -> originalRegistry
            else -> "https://$originalRegistry"
        }
    }

    /**
     * Add a custom mirror
     */
    suspend fun addCustomMirror(name: String, url: String, bearerToken: String? = null, priority: Int = 50) {
        ensureInitialized()

        val newMirror = MirrorEntity(
            url = url.removeSuffix("/"),
            name = name,
            bearerToken = bearerToken,
            priority = priority,
            isBuiltIn = false,
            isHealthy = true,
            latencyMs = -1,
            lastChecked = 0
        )

        mirrorDao.insertMirror(newMirror)

        // Reset failure count and check health immediately
        healthChecker.resetFailureCount(newMirror.url)
        healthChecker.checkMirror(newMirror)

        Timber.i("Added custom mirror: $name ($url)")
    }

    /**
     * Update mirror bearer token
     */
    suspend fun updateMirrorToken(url: String, bearerToken: String?) {
        val mirror = mirrorDao.getMirrorByUrl(url) ?: return
        val updatedMirror = mirror.copy(bearerToken = bearerToken)
        mirrorDao.insertMirror(updatedMirror)
        Timber.i("Updated bearer token for mirror: ${mirror.name}")
    }

    /**
     * Delete a custom mirror
     */
    suspend fun deleteCustomMirror(mirror: MirrorEntity) {
        if (mirror.isBuiltIn) {
            Timber.w("Cannot delete built-in mirror: ${mirror.name}")
            return
        }

        ensureInitialized()
        mirrorDao.deleteMirrorByUrl(mirror.url)
        Timber.i("Deleted custom mirror: ${mirror.name}")
    }

    /**
     * Get bearer token for a specific mirror URL
     */
    suspend fun getBearerToken(mirrorUrl: String): String? {
        val mirror = mirrorDao.getMirrorByUrl(mirrorUrl)
        return mirror?.bearerToken
    }

    /**
     * Check if currently using a mirror (not Docker Hub)
     */
    suspend fun isUsingMirror(): Boolean {
        val currentUrl = getBestMirror()
        return currentUrl != DOCKER_HUB_URL
    }

    /**
     * Trigger immediate health check of all mirrors
     */
    fun checkMirrorsNow() {
        healthChecker.checkNow()
    }
}
