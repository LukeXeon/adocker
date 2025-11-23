package com.adocker.runner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adocker.runner.core.config.Config
import com.adocker.runner.core.config.RegistrySettings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BasicFunctionalityTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        Config.init(context)
        RegistrySettings.init(context)
    }

    @Test
    fun config_isInitializedProperly() {
        assertNotNull("Context should be initialized", context)
        assertTrue("Base directory should exist", Config.baseDir.exists())
        assertTrue("Bin directory should exist", Config.binDir.exists())
        assertTrue("Containers directory should exist", Config.containersDir.exists())
        assertTrue("Layers directory should exist", Config.layersDir.exists())
    }

    @Test
    fun registrySettings_hasDefaultMirror() = runBlocking {
        val currentMirror = RegistrySettings.getCurrentMirror()
        assertNotNull("Default mirror should be set", currentMirror)
        assertEquals("DaoCloud should be default", "DaoCloud (China)", currentMirror.name)
        assertTrue("Mirror URL should not be empty", currentMirror.url.isNotEmpty())
    }

    @Test
    fun registrySettings_hasBuiltInMirrors() = runBlocking {
        val allMirrors = RegistrySettings.getAllMirrors()
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
        RegistrySettings.addCustomMirror(testName, testUrl)

        // Verify it was added
        var allMirrors = RegistrySettings.getAllMirrors()
        val addedMirror = allMirrors.find { it.name == testName }
        assertNotNull("Custom mirror should be added", addedMirror)
        assertFalse("Custom mirror should not be built-in", addedMirror!!.isBuiltIn)
        assertEquals("Custom mirror URL should match", testUrl, addedMirror.url)

        // Delete custom mirror
        RegistrySettings.deleteCustomMirror(addedMirror)

        // Verify it was deleted
        allMirrors = RegistrySettings.getAllMirrors()
        val deletedMirror = allMirrors.find { it.name == testName }
        assertNull("Custom mirror should be deleted", deletedMirror)
    }

    @Test
    fun registrySettings_cannotDeleteBuiltInMirror() = runBlocking {
        val initialCount = RegistrySettings.getAllMirrors().size
        val builtInMirror = RegistrySettings.BUILT_IN_MIRRORS.first()

        // Try to delete built-in mirror
        RegistrySettings.deleteCustomMirror(builtInMirror)

        // Verify it still exists
        val finalCount = RegistrySettings.getAllMirrors().size
        assertEquals("Built-in mirrors should not be deleted", initialCount, finalCount)
    }

    @Test
    fun registrySettings_canSwitchMirrors() = runBlocking {
        val dockerHubMirror = RegistrySettings.BUILT_IN_MIRRORS.find { it.name.contains("Docker Hub") }
        assertNotNull("Docker Hub mirror should exist", dockerHubMirror)

        // Switch to Docker Hub
        RegistrySettings.setMirror(dockerHubMirror!!)

        // Verify switch
        val currentMirror = RegistrySettings.getCurrentMirror()
        assertEquals("Should be Docker Hub", dockerHubMirror.url, currentMirror.url)

        // Switch back to default
        RegistrySettings.setMirror(RegistrySettings.getDefaultMirror())
        val resetMirror = RegistrySettings.getCurrentMirror()
        assertEquals("Should be back to default", RegistrySettings.getDefaultMirror().url, resetMirror.url)
    }
}
