package com.adocker.runner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.adocker.runner.core.config.AppConfig
import com.adocker.runner.core.config.RegistrySettingsManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class BasicFunctionalityTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var appConfig: AppConfig

    @Inject
    lateinit var registrySettings: RegistrySettingsManager

    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun config_isInitializedProperly() {
        assertNotNull("Context should be initialized", context)
        assertTrue("Base directory should exist", appConfig.baseDir.exists())
        assertTrue("Bin directory should exist", appConfig.binDir.exists())
        assertTrue("Containers directory should exist", appConfig.containersDir.exists())
        assertTrue("Layers directory should exist", appConfig.layersDir.exists())
    }

    @Test
    fun registrySettings_hasDefaultMirror() = runBlocking {
        val currentMirror = registrySettings.getCurrentMirror()
        assertNotNull("Default mirror should be set", currentMirror)
        assertEquals("DaoCloud should be default", "DaoCloud (China)", currentMirror.name)
        assertTrue("Mirror URL should not be empty", currentMirror.url.isNotEmpty())
    }

    @Test
    fun registrySettings_hasBuiltInMirrors() = runBlocking {
        val allMirrors = registrySettings.getAllMirrors()
        assertTrue("Should have at least 5 built-in mirrors", allMirrors.size >= 5)

        val mirrorNames = allMirrors.map { it.name }
        assertTrue("Should have Docker Hub", mirrorNames.any { it.contains("Docker Hub") })
        assertTrue("Should have Xuanyuan", mirrorNames.any { it.contains("Xuanyuan") })
        assertTrue("Should have DaoCloud", mirrorNames.any { it.contains("DaoCloud") })
        assertTrue("Should have Aliyun", mirrorNames.any { it.contains("Aliyun") })
        assertTrue("Should have Huawei", mirrorNames.any { it.contains("Huawei") })
        // Note: USTC and Tencent Cloud mirrors removed due to connectivity issues in China
    }

    @Test
    fun registrySettings_canAddAndDeleteCustomMirror() = runBlocking {
        val testName = "Test Mirror ${System.currentTimeMillis()}"
        val testUrl = "https://test.example.com"

        // Add custom mirror
        registrySettings.addCustomMirror(testName, testUrl)

        // Verify it was added
        var allMirrors = registrySettings.getAllMirrors()
        val addedMirror = allMirrors.find { it.name == testName }
        assertNotNull("Custom mirror should be added", addedMirror)
        assertFalse("Custom mirror should not be built-in", addedMirror!!.isBuiltIn)
        assertEquals("Custom mirror URL should match", testUrl, addedMirror.url)

        // Delete custom mirror
        registrySettings.deleteCustomMirror(addedMirror)

        // Verify it was deleted
        allMirrors = registrySettings.getAllMirrors()
        val deletedMirror = allMirrors.find { it.name == testName }
        assertNull("Custom mirror should be deleted", deletedMirror)
    }

    @Test
    fun registrySettings_cannotDeleteBuiltInMirror() = runBlocking {
        val initialCount = registrySettings.getAllMirrors().size
        val builtInMirror = RegistrySettingsManager.BUILT_IN_MIRRORS.first()

        // Try to delete built-in mirror
        registrySettings.deleteCustomMirror(builtInMirror)

        // Verify it still exists
        val finalCount = registrySettings.getAllMirrors().size
        assertEquals("Built-in mirrors should not be deleted", initialCount, finalCount)
    }

    @Test
    fun registrySettings_canSwitchMirrors() = runBlocking {
        val dockerHubMirror = RegistrySettingsManager.BUILT_IN_MIRRORS.find { it.name.contains("Docker Hub") }
        assertNotNull("Docker Hub mirror should exist", dockerHubMirror)

        // Switch to Docker Hub
        registrySettings.setMirror(dockerHubMirror!!)

        // Verify switch
        val currentMirror = registrySettings.getCurrentMirror()
        assertEquals("Should be Docker Hub", dockerHubMirror.url, currentMirror.url)

        // Switch back to default
        registrySettings.setMirror(RegistrySettingsManager.getDefaultMirror())
        val resetMirror = registrySettings.getCurrentMirror()
        assertEquals("Should be back to default", RegistrySettingsManager.getDefaultMirror().url, resetMirror.url)
    }
}
