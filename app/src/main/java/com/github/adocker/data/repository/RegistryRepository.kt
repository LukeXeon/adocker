package com.github.adocker.data.repository

import com.github.adocker.data.local.dao.MirrorDao
import com.github.adocker.data.local.model.MirrorEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registry settings manager - dependency injected version
 *
 * Manages Docker registry mirrors with support for custom mirrors
 * Now using Room database for persistence
 */

@Singleton
class RegistryRepository @Inject constructor(
    private val mirrorDao: MirrorDao
) {
    /**
     * Initialize built-in mirrors on first use
     * Using database as source of truth - no need for separate initialized flag
     */
    private suspend fun ensureInitialized() {
        // Check if database already has mirrors
        val existingMirrors = mirrorDao.getAllMirrors().first()
        if (existingMirrors.isEmpty()) {
            // Insert built-in mirrors
            mirrorDao.insertMirrors(BUILT_IN_MIRRORS)

            // Set default mirror as selected
            val defaultMirror = BUILT_IN_MIRRORS.find { it.isDefault }
            if (defaultMirror != null) {
                mirrorDao.selectMirrorByUrl(defaultMirror.url)
            }
        }
    }

    /**
     * Get all mirrors (built-in + custom)
     */
    suspend fun getAllMirrors(): List<MirrorEntity> {
        ensureInitialized()
        return mirrorDao.getAllMirrors().first()
    }

    /**
     * Get all mirrors flow for observing changes
     */
    fun getAllMirrorsFlow(): Flow<List<MirrorEntity>> {
        return mirrorDao.getAllMirrors()
    }

    /**
     * Get current registry mirror
     */
    suspend fun getCurrentMirror(): MirrorEntity {
        ensureInitialized()

        val selectedMirror = mirrorDao.getSelectedMirror()
        if (selectedMirror != null) {
            return selectedMirror
        }

        // No mirror selected, use default
        val defaultMirror = getDefaultMirror()
        setMirror(defaultMirror)
        return defaultMirror
    }

    /**
     * Get current mirror flow for observing changes
     */
    fun getCurrentMirrorFlow(): Flow<MirrorEntity> {
        return mirrorDao.getSelectedMirrorFlow().map { entity ->
            entity ?: getDefaultMirror()
        }
    }

    /**
     * Set registry mirror
     */
    suspend fun setMirror(mirror: MirrorEntity) {
        ensureInitialized()

        // Clear previous selection
        mirrorDao.clearAllSelected()

        // Select new mirror
        mirrorDao.selectMirrorByUrl(mirror.url)
    }

    /**
     * Add a custom mirror
     */
    suspend fun addCustomMirror(name: String, url: String, bearerToken: String? = null) {
        ensureInitialized()

        val newMirror = MirrorEntity(
            url = url.removeSuffix("/"),
            name = name,
            bearerToken = bearerToken,
            isDefault = false,
            isBuiltIn = false,
            isSelected = false
        )

        mirrorDao.insertMirror(newMirror)
    }

    /**
     * Update mirror bearer token
     */
    suspend fun updateMirrorToken(url: String, bearerToken: String?) {
        val mirror = mirrorDao.getAllMirrors().first().find { it.url == url } ?: return
        val updatedMirror = mirror.copy(bearerToken = bearerToken)
        mirrorDao.insertMirror(updatedMirror)
    }

    /**
     * Delete a custom mirror
     */
    suspend fun deleteCustomMirror(mirror: MirrorEntity) {
        if (mirror.isBuiltIn) return // Cannot delete built-in mirrors

        ensureInitialized()

        // If the deleted mirror was selected, switch to default
        val currentMirror = mirrorDao.getSelectedMirror()
        if (currentMirror?.url == mirror.url) {
            val defaultMirror = getDefaultMirror()
            setMirror(defaultMirror)
        }

        mirrorDao.deleteMirrorByUrl(mirror.url)
    }

    /**
     * Get registry URL for pulling images
     * Replaces Docker Hub URLs with mirror URL
     */
    suspend fun getRegistryUrl(originalRegistry: String): String {
        val mirror = getCurrentMirror()

        return when {
            // Docker Hub - use mirror
            originalRegistry == "docker.io" ||
                    originalRegistry == "registry-1.docker.io" ||
                    originalRegistry.contains("docker.io") -> mirror.url

            // Other registries - use as-is
            originalRegistry.startsWith("http") -> originalRegistry
            else -> "https://$originalRegistry"
        }
    }

    /**
     * Check if using a mirror (not official Docker Hub)
     */
    suspend fun isUsingMirror(): Boolean {
        val mirror = getCurrentMirror()
        return mirror.url != "https://registry-1.docker.io"
    }

    companion object {
        // Built-in mirrors for Docker Hub
        val BUILT_IN_MIRRORS = listOf(
            MirrorEntity(
                name = "Docker Hub (Official)",
                url = "https://registry-1.docker.io",
                isDefault = false,
                isBuiltIn = true
            ),
            MirrorEntity(
                name = "DaoCloud (China)",
                url = "https://docker.m.daocloud.io",
                isDefault = true,  // Default - supports token auth
                isBuiltIn = true
            ),
            MirrorEntity(
                name = "Xuanyuan (China)",
                url = "https://docker.xuanyuan.me",
                isDefault = false,
                isBuiltIn = true
            ),
            MirrorEntity(
                name = "Aliyun (China)",
                url = "https://registry.cn-hangzhou.aliyuncs.com",
                isDefault = false,
                isBuiltIn = true
            ),
            MirrorEntity(
                name = "Huawei Cloud (China)",
                url = "https://mirrors.huaweicloud.com",
                isDefault = false,
                isBuiltIn = true
            )
        )

        // Backwards compatible alias
        val AVAILABLE_MIRRORS get() = BUILT_IN_MIRRORS

        /**
         * Get default mirror (DaoCloud for China)
         */
        fun getDefaultMirror(): MirrorEntity {
            return AVAILABLE_MIRRORS.find { it.isDefault } ?: AVAILABLE_MIRRORS.first()
        }
    }
}