package com.github.andock.daemon.server.handlers

import android.app.Application
import android.os.Build
import com.github.andock.common.isDebuggable
import com.github.andock.common.nativeLibDir
import com.github.andock.common.packageInfo
import com.github.andock.daemon.app.containersDir
import com.github.andock.daemon.app.layersDir
import com.github.andock.daemon.containers.ContainerManager
import com.github.andock.daemon.containers.ContainerState
import com.github.andock.daemon.images.ImageManager
import com.github.andock.daemon.server.models.Commit
import com.github.andock.daemon.server.models.ContainerDiskUsage
import com.github.andock.daemon.server.models.DiskUsage
import com.github.andock.daemon.server.models.ImageDiskUsage
import com.github.andock.daemon.server.models.Plugins
import com.github.andock.daemon.server.models.SwarmInfo
import com.github.andock.daemon.server.models.SystemInfo
import com.github.andock.daemon.server.models.VersionInfo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.http4k.core.Response
import org.http4k.core.Status
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemHandler @Inject constructor(
    private val appContext: Application,
    private val containerManager: ContainerManager,
    private val imageManager: ImageManager
) {
    fun getVersion(): Response {
        val version = VersionInfo(
            version = appContext.packageInfo.versionName ?: "unknown",
            apiVersion = "1.45",
            minAPIVersion = "1.12",
            gitCommit = "unknown",
            goVersion = "go1.21.0",
            os = "android",
            arch = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
            kernelVersion = System.getProperty("os.version") ?: "unknown",
            buildTime = "unknown"
        )
        return Response.Companion(Status.OK)
            .header("Content-Type", "application/json")
            .body(Json.encodeToString(version))
    }

    fun getInfo(): Response = runBlocking {
        val containers = containerManager.containers.value.values.toList()
        val images = imageManager.images.first()

        val containersRunning = containers.count {
            it.state.value is ContainerState.Running
        }
        val containersStopped = containers.size - containersRunning

        val info = SystemInfo(
            id = "andock-" + appContext.packageInfo.packageName,
            containers = containers.size,
            containersRunning = containersRunning,
            containersPaused = 0,
            containersStopped = containersStopped,
            images = images.size,
            driver = "proot",
            driverStatus = listOf(
                listOf("Driver", "PRoot"),
                listOf("Root Dir", appContext.containersDir.absolutePath)
            ),
            systemStatus = null,
            plugins = Plugins(
                volume = listOf("local"),
                network = listOf("bridge", "host", "none"),
                authorization = null,
                log = listOf("json-file")
            ),
            memoryLimit = false,
            swapLimit = false,
            kernelMemory = false,
            cpuCfsPeriod = false,
            cpuCfsQuota = false,
            cpuShares = false,
            cpuSet = false,
            pidsLimit = false,
            oomKillDisable = false,
            ipv4Forwarding = false,
            bridgeNfIptables = false,
            bridgeNfIp6tables = false,
            debug = appContext.isDebuggable,
            nFd = 0,
            nGoroutines = 0,
            systemTime = Instant.now().toString(),
            loggingDriver = "json-file",
            cgroupDriver = "none",
            nEventsListener = 0,
            kernelVersion = System.getProperty("os.version") ?: "unknown",
            operatingSystem = "Android ${Build.VERSION.RELEASE}",
            osType = "android",
            architecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
            ncpu = Runtime.getRuntime().availableProcessors(),
            memTotal = Runtime.getRuntime().maxMemory(),
            indexServerAddress = "https://index.docker.io/v1/",
            registryConfig = null,
            genericResources = null,
            httpProxy = "",
            httpsProxy = "",
            noProxy = "",
            name = Build.MODEL,
            labels = emptyList(),
            experimentalBuild = false,
            serverVersion = appContext.packageInfo.versionName ?: "unknown",
            clusterStore = "",
            clusterAdvertise = "",
            runtimes = mapOf(
                "proot" to com.github.andock.daemon.server.models.Runtime(
                    path = appContext.nativeLibDir.absolutePath + "/libproot.so",
                    runtimeArgs = null
                )
            ),
            defaultRuntime = "proot",
            swarm = SwarmInfo(
                nodeID = "",
                nodeAddr = "",
                localNodeState = "inactive",
                controlAvailable = false,
                error = "",
                remoteManagers = null
            ),
            liveRestoreEnabled = false,
            isolation = "",
            initBinary = "",
            containerdCommit = Commit(id = "unknown", expected = "unknown"),
            runcCommit = Commit(id = "unknown", expected = "unknown"),
            initCommit = Commit(id = "unknown", expected = "unknown"),
            securityOptions = listOf("name=proot"),
            warnings = null
        )
        return@runBlocking Response.Companion(Status.OK)
            .header("Content-Type", "application/json")
            .body(Json.encodeToString(info))
    }

    fun ping(): Response {
        return Response.Companion(Status.OK)
            .header("Api-Version", "1.45")
            .header("Docker-Experimental", "false")
            .body("OK")
    }

    fun getDiskUsage(): Response = runBlocking {
        val images = imageManager.images.first()
        val containers = containerManager.containers.value.values.toList()

        // Calculate layers size
        val layersDir = appContext.layersDir
        val layersSize = layersDir.listFiles()?.sumOf { it.length() } ?: 0L

        val imageDiskUsage = images.map { image ->
            ImageDiskUsage(
                id = image.id,
                parentId = "",
                repoTags = listOf("${image.repository}:${image.tag}"),
                repoDigests = listOf(image.id),
                created = image.created / 1000,
                size = image.size,
                sharedSize = 0,
                virtualSize = image.size,
                labels = null,
                containers = 0
            )
        }

        val containerDiskUsage = containers.mapNotNull { container ->
            val metadata = container.metadata.value ?: return@mapNotNull null
            val state = container.state.value
            val stateStr = when (state) {
                is ContainerState.Running -> "running"
                is ContainerState.Exited -> "exited"
                is ContainerState.Created -> "created"
                else -> "unknown"
            }

            val containerDir = File(appContext.containersDir, container.id)
            val sizeRootFs = containerDir.walkTopDown().sumOf { it.length() }

            ContainerDiskUsage(
                id = container.id,
                names = listOf("/" + metadata.name),
                image = metadata.imageName,
                imageID = metadata.imageId,
                created = metadata.createdAt / 1000,
                sizeRw = 0,
                sizeRootFs = sizeRootFs,
                state = stateStr,
                status = stateStr
            )
        }

        val diskUsage = DiskUsage(
            layersSize = layersSize,
            images = imageDiskUsage,
            containers = containerDiskUsage,
            volumes = emptyList()
        )

        return@runBlocking Response.Companion(Status.OK)
            .header("Content-Type", "application/json")
            .body(Json.encodeToString(diskUsage))
    }
}