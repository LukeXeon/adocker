package com.github.andock.daemon.server.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SystemInfo(
    @SerialName("ID")
    val id: String,
    @SerialName("Containers")
    val containers: Int,
    @SerialName("ContainersRunning")
    val containersRunning: Int,
    @SerialName("ContainersPaused")
    val containersPaused: Int,
    @SerialName("ContainersStopped")
    val containersStopped: Int,
    @SerialName("Images")
    val images: Int,
    @SerialName("Driver")
    val driver: String,
    @SerialName("DriverStatus")
    val driverStatus: List<List<String>>,
    @SerialName("SystemStatus")
    val systemStatus: List<List<String>>?,
    @SerialName("Plugins")
    val plugins: Plugins,
    @SerialName("MemoryLimit")
    val memoryLimit: Boolean,
    @SerialName("SwapLimit")
    val swapLimit: Boolean,
    @SerialName("KernelMemory")
    val kernelMemory: Boolean,
    @SerialName("CpuCfsPeriod")
    val cpuCfsPeriod: Boolean,
    @SerialName("CpuCfsQuota")
    val cpuCfsQuota: Boolean,
    @SerialName("CPUShares")
    val cpuShares: Boolean,
    @SerialName("CPUSet")
    val cpuSet: Boolean,
    @SerialName("PidsLimit")
    val pidsLimit: Boolean,
    @SerialName("OomKillDisable")
    val oomKillDisable: Boolean,
    @SerialName("IPv4Forwarding")
    val ipv4Forwarding: Boolean,
    @SerialName("BridgeNfIptables")
    val bridgeNfIptables: Boolean,
    @SerialName("BridgeNfIp6tables")
    val bridgeNfIp6tables: Boolean,
    @SerialName("Debug")
    val debug: Boolean,
    @SerialName("NFd")
    val nFd: Int,
    @SerialName("NGoroutines")
    val nGoroutines: Int,
    @SerialName("SystemTime")
    val systemTime: String,
    @SerialName("LoggingDriver")
    val loggingDriver: String,
    @SerialName("CgroupDriver")
    val cgroupDriver: String,
    @SerialName("NEventsListener")
    val nEventsListener: Int,
    @SerialName("KernelVersion")
    val kernelVersion: String,
    @SerialName("OperatingSystem")
    val operatingSystem: String,
    @SerialName("OSType")
    val osType: String,
    @SerialName("Architecture")
    val architecture: String,
    @SerialName("NCPU")
    val ncpu: Int,
    @SerialName("MemTotal")
    val memTotal: Long,
    @SerialName("IndexServerAddress")
    val indexServerAddress: String,
    @SerialName("RegistryConfig")
    val registryConfig: RegistryConfig?,
    @SerialName("GenericResources")
    val genericResources: List<String>?,
    @SerialName("HttpProxy")
    val httpProxy: String,
    @SerialName("HttpsProxy")
    val httpsProxy: String,
    @SerialName("NoProxy")
    val noProxy: String,
    @SerialName("Name")
    val name: String,
    @SerialName("Labels")
    val labels: List<String>,
    @SerialName("ExperimentalBuild")
    val experimentalBuild: Boolean,
    @SerialName("ServerVersion")
    val serverVersion: String,
    @SerialName("ClusterStore")
    val clusterStore: String,
    @SerialName("ClusterAdvertise")
    val clusterAdvertise: String,
    @SerialName("Runtimes")
    val runtimes: Map<String, Runtime>,
    @SerialName("DefaultRuntime")
    val defaultRuntime: String,
    @SerialName("Swarm")
    val swarm: SwarmInfo,
    @SerialName("LiveRestoreEnabled")
    val liveRestoreEnabled: Boolean,
    @SerialName("Isolation")
    val isolation: String,
    @SerialName("InitBinary")
    val initBinary: String,
    @SerialName("ContainerdCommit")
    val containerdCommit: Commit,
    @SerialName("RuncCommit")
    val runcCommit: Commit,
    @SerialName("InitCommit")
    val initCommit: Commit,
    @SerialName("SecurityOptions")
    val securityOptions: List<String>,
    @SerialName("Warnings")
    val warnings: List<String>?
)

