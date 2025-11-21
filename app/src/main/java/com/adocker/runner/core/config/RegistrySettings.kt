package com.adocker.runner.core.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "registry_settings")

/**
 * Registry settings with mirror support for China mainland users
 */
object RegistrySettings {
    private val REGISTRY_MIRROR_KEY = stringPreferencesKey("registry_mirror")
    private val CUSTOM_MIRRORS_KEY = stringPreferencesKey("custom_mirrors")

    // Built-in mirrors for Docker Hub
    val BUILT_IN_MIRRORS = listOf(
        RegistryMirror(
            name = "Docker Hub (Official)",
            url = "https://registry-1.docker.io",
            authUrl = "https://auth.docker.io",
            isDefault = false,
            isBuiltIn = true
        ),
        RegistryMirror(
            name = "DaoCloud (China)",
            url = "https://docker.m.daocloud.io",
            authUrl = "https://auth.docker.io",
            isDefault = true,  // Default for China users
            isBuiltIn = true
        ),
        RegistryMirror(
            name = "Aliyun (China)",
            url = "https://registry.cn-hangzhou.aliyuncs.com",
            authUrl = "https://auth.docker.io",
            isDefault = false,
            isBuiltIn = true
        ),
        RegistryMirror(
            name = "USTC (China)",
            url = "https://docker.mirrors.ustc.edu.cn",
            authUrl = "https://auth.docker.io",
            isDefault = false,
            isBuiltIn = true
        ),
        RegistryMirror(
            name = "Tencent Cloud (China)",
            url = "https://mirror.ccs.tencentyun.com",
            authUrl = "https://auth.docker.io",
            isDefault = false,
            isBuiltIn = true
        ),
        RegistryMirror(
            name = "Huawei Cloud (China)",
            url = "https://mirrors.huaweicloud.com",
            authUrl = "https://auth.docker.io",
            isDefault = false,
            isBuiltIn = true
        )
    )

    // Backwards compatible alias
    val AVAILABLE_MIRRORS get() = BUILT_IN_MIRRORS

    private var appContext: Context? = null
    private var cachedMirror: RegistryMirror? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Get all mirrors (built-in + custom)
     */
    suspend fun getAllMirrors(): List<RegistryMirror> {
        val customMirrors = getCustomMirrors()
        return BUILT_IN_MIRRORS + customMirrors
    }

    /**
     * Get all mirrors flow for observing changes
     */
    fun getAllMirrorsFlow(): Flow<List<RegistryMirror>> {
        val context = appContext ?: throw IllegalStateException("RegistrySettings not initialized")
        return context.dataStore.data.map { prefs ->
            val customJson = prefs[CUSTOM_MIRRORS_KEY] ?: "[]"
            val customMirrors = parseCustomMirrors(customJson)
            BUILT_IN_MIRRORS + customMirrors
        }
    }

    /**
     * Get current registry mirror URL
     */
    suspend fun getCurrentMirror(): RegistryMirror {
        cachedMirror?.let { return it }

        val context = appContext ?: return getDefaultMirror()
        val prefs = context.dataStore.data.first()
        val mirrorUrl = prefs[REGISTRY_MIRROR_KEY]

        val allMirrors = getAllMirrors()
        val mirror = if (mirrorUrl != null) {
            allMirrors.find { it.url == mirrorUrl } ?: getDefaultMirror()
        } else {
            // First run - set default mirror for China
            val defaultMirror = getDefaultMirror()
            setMirror(defaultMirror)
            defaultMirror
        }

        cachedMirror = mirror
        return mirror
    }

    /**
     * Get current mirror flow for observing changes
     */
    fun getCurrentMirrorFlow(): Flow<RegistryMirror> {
        val context = appContext ?: throw IllegalStateException("RegistrySettings not initialized")
        return context.dataStore.data.map { prefs ->
            val mirrorUrl = prefs[REGISTRY_MIRROR_KEY]
            val customJson = prefs[CUSTOM_MIRRORS_KEY] ?: "[]"
            val customMirrors = parseCustomMirrors(customJson)
            val allMirrors = BUILT_IN_MIRRORS + customMirrors

            if (mirrorUrl != null) {
                allMirrors.find { it.url == mirrorUrl } ?: getDefaultMirror()
            } else {
                getDefaultMirror()
            }
        }
    }

    /**
     * Set registry mirror
     */
    suspend fun setMirror(mirror: RegistryMirror) {
        val context = appContext ?: return
        context.dataStore.edit { prefs ->
            prefs[REGISTRY_MIRROR_KEY] = mirror.url
        }
        cachedMirror = mirror
    }

    /**
     * Add a custom mirror
     */
    suspend fun addCustomMirror(name: String, url: String) {
        val context = appContext ?: return
        val newMirror = RegistryMirror(
            name = name,
            url = url.removeSuffix("/"),
            authUrl = "https://auth.docker.io",
            isDefault = false,
            isBuiltIn = false
        )

        context.dataStore.edit { prefs ->
            val currentJson = prefs[CUSTOM_MIRRORS_KEY] ?: "[]"
            val currentMirrors = parseCustomMirrors(currentJson).toMutableList()
            // Avoid duplicates by URL
            if (currentMirrors.none { it.url == newMirror.url }) {
                currentMirrors.add(newMirror)
                prefs[CUSTOM_MIRRORS_KEY] = serializeCustomMirrors(currentMirrors)
            }
        }
    }

    /**
     * Delete a custom mirror
     */
    suspend fun deleteCustomMirror(mirror: RegistryMirror) {
        if (mirror.isBuiltIn) return // Cannot delete built-in mirrors

        val context = appContext ?: return
        context.dataStore.edit { prefs ->
            val currentJson = prefs[CUSTOM_MIRRORS_KEY] ?: "[]"
            val currentMirrors = parseCustomMirrors(currentJson).toMutableList()
            currentMirrors.removeAll { it.url == mirror.url }
            prefs[CUSTOM_MIRRORS_KEY] = serializeCustomMirrors(currentMirrors)

            // If the deleted mirror was selected, switch to default
            val selectedUrl = prefs[REGISTRY_MIRROR_KEY]
            if (selectedUrl == mirror.url) {
                prefs[REGISTRY_MIRROR_KEY] = getDefaultMirror().url
                cachedMirror = null
            }
        }
    }

    /**
     * Get custom mirrors
     */
    private suspend fun getCustomMirrors(): List<RegistryMirror> {
        val context = appContext ?: return emptyList()
        val prefs = context.dataStore.data.first()
        val json = prefs[CUSTOM_MIRRORS_KEY] ?: return emptyList()
        return parseCustomMirrors(json)
    }

    /**
     * Parse custom mirrors from JSON string
     * Format: "name1|url1;name2|url2;..."
     */
    private fun parseCustomMirrors(data: String): List<RegistryMirror> {
        if (data.isBlank() || data == "[]") return emptyList()
        return try {
            data.split(";").mapNotNull { entry ->
                val parts = entry.split("|")
                if (parts.size >= 2) {
                    RegistryMirror(
                        name = parts[0],
                        url = parts[1],
                        authUrl = "https://auth.docker.io",
                        isDefault = false,
                        isBuiltIn = false
                    )
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Serialize custom mirrors to string
     */
    private fun serializeCustomMirrors(mirrors: List<RegistryMirror>): String {
        return mirrors.joinToString(";") { "${it.name}|${it.url}" }
    }

    /**
     * Get default mirror (DaoCloud for China)
     */
    fun getDefaultMirror(): RegistryMirror {
        return AVAILABLE_MIRRORS.find { it.isDefault } ?: AVAILABLE_MIRRORS.first()
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
}

data class RegistryMirror(
    val name: String,
    val url: String,
    val authUrl: String,
    val isDefault: Boolean = false,
    val isBuiltIn: Boolean = true
)
