# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Andock** (package: `com.github.andock`) is an Android application that runs Docker containers without root privileges using PRoot (user-space chroot). It's a complete Kotlin reimplementation of the udocker concept, designed specifically for Android with full internationalization support (Chinese/English).

**Key Technologies:**
- Kotlin 2.2.21 + Jetpack Compose (Material Design 3, BOM 2025.12.00)
- Hilt 2.57.2 dependency injection with AssistedInject
- Ktor 3.3.3 HTTP client (OkHttp engine)
- Room 2.8.4 database
- FlowRedux 2.0.0 state machines (containers, downloads, registries)
- PRoot v0.15 execution engine (from green-green-avk/proot)
- Coroutines 1.10.2 & Flow for async/reactive programming
- Paging 3.3.5 for Docker Hub search
- CameraX 1.5.2 + ML Kit 17.3.0 for QR code scanning
- Shizuku 13.1.5 for system service integration
- http4k 6.23.1.0 for Docker API server

## Build & Development Commands

### Build the app
```bash
./gradlew assembleDebug
```

### Run instrumented tests
```bash
# Full test suite (requires device/emulator with ADB access)
./gradlew connectedAndroidTest

# Specific test
./gradlew connectedDebugAndroidTest --tests com.github.adocker.ImagePullAndRunTest
```

**Test requirements:**
- Physical device or emulator connected via ADB
- ADB must be in PATH: `export PATH="$PATH:$HOME/Library/Android/sdk/platform-tools"`
- JDK 17+ required: `export JAVA_HOME=/opt/homebrew/opt/openjdk@17` (macOS)
- Tests requiring PRoot will be skipped if binary is not available on the architecture

### Install and run on device
```bash
# Set up ADB path first
export PATH="$PATH:$HOME/Library/Android/sdk/platform-tools"

# Install
adb install app/build/outputs/apk/debug/app-debug.apk

# View logs
adb logcat -d | grep -i "PRootEngine\|ImagePull\|Container"
```

### Verify PRoot availability
PRoot binaries are pre-built and stored in `app/src/main/jniLibs/{arch}/` as:
- `libproot.so` - PRoot binary
- `libproot_loader.so` - 64-bit loader
- `libproot_loader32.so` - 32-bit loader

Check if they exist:
```bash
ls -la app/src/main/jniLibs/arm64-v8a/
```

## Architecture

### Module Structure

The project uses a multi-module architecture with clear separation:

```
andock/
├── daemon/                           # Core business logic module (Android Library)
│   └── src/main/java/com/github/andock/daemon/
│       ├── app/                      # AppContext, AppInitializer, AppModule
│       ├── containers/               # Container, ContainerManager, ContainerState, ContainerStateMachine
│       ├── database/                 # Room database, DAOs, entities
│       │   ├── dao/                  # ImageDao, LayerDao, ContainerDao, RegistryDao, AuthTokenDao, SearchRecordDao
│       │   └── model/                # ImageEntity, LayerEntity, ContainerEntity, RegistryEntity, etc.
│       ├── engine/                   # PRootEngine, PRootEnvironment, PRootVersion
│       ├── images/                   # ImageManager, ImageClient, ImageReference
│       │   ├── downloader/           # ImageDownloader, ImageDownloadState (FlowRedux)
│       │   └── model/                # Registry API models (manifests, auth, configs)
│       ├── http/                     # UnixHttp4kServer, TcpHttp4kServer, HttpProcessor
│       ├── server/                   # DockerApiServer
│       │   └── routes/               # ContainerRoutes, ImageRoutes, SystemRoutes, etc. (Hilt multibinding)
│       ├── registries/               # Registry, RegistryManager, RegistryStateMachine (FlowRedux)
│       ├── search/                   # SearchPagingSource, SearchRepository, SearchHistory (Paging 3 + DataStore)
│       │   └── model/                # SearchResponse, SearchResult
│       ├── io/                       # File, Zip, Tailer (symlink handling, tar extraction)
│       ├── os/                       # ProcessLimitCompat, RemoteProcess, Process (Shizuku integration)
│       └── logging/                  # TimberLogger, TimberServiceProvider (SLF4J + Timber)
└── app/                              # UI module (Android Application)
    └── src/main/java/com/github/andock/
        ├── AndockApplication.kt      # Application class
        └── ui/
            ├── MainActivity.kt       # Single Activity + Compose
            ├── screens/              # Screen composables (by feature)
            │   ├── main/             # MainScreen (bottom navigation)
            │   ├── home/             # HomeScreen, HomeViewModel
            │   ├── containers/       # ContainersScreen, ContainerDetailScreen, ContainerCreateScreen, etc.
            │   ├── images/           # ImagesScreen, ImageDetailScreen, ImagePullDialog, etc.
            │   ├── search/           # SearchScreen, SearchFilterPanel, SearchHistoryPanel, etc.
            │   ├── registries/       # RegistriesScreen, AddMirrorScreen, etc.
            │   ├── qrcode/           # QrcodeScannerScreen, QrcodeCamera
            │   ├── terminal/         # TerminalScreen, TerminalViewModel
            │   ├── limits/           # ProcessLimitScreen
            │   └── settings/         # SettingsScreen, SettingsViewModel
            ├── components/           # Reusable UI components (DetailCard, StatusIndicator, etc.)
            └── theme/                # Spacing.kt, IconSize.kt (Material Design 3)
```

### Package Structure (daemon module)

```
com.github.andock.daemon/
├── app/
│   ├── AppContext.kt             # Centralized app configuration (directories, constants)
│   ├── AppInitializer.kt         # App Startup initialization
│   └── AppModule.kt              # Hilt module (provides DB, HTTP client, etc.)
│
├── containers/
│   ├── Container.kt              # Container instance with FlowRedux state machine
│   ├── ContainerManager.kt       # Container creation, tracking, deletion
│   ├── ContainerState.kt         # 8 states: Created, Starting, Running, Stopping, Exited, Dead, Removing, Removed
│   ├── ContainerStateMachine.kt  # FlowRedux state machine for container lifecycle
│   ├── ContainerOperation.kt     # Operations: Start, Stop, Exec, Remove
│   ├── ContainerProcess.kt       # Wrapper for container processes
│   ├── ContainerName.kt          # Random container name generator
│   └── ContainerModule.kt        # Hilt module
│
├── database/
│   ├── AppDatabase.kt            # Room database (version 1)
│   ├── Converters.kt             # Type converters for Room
│   ├── DatabaseModule.kt         # Hilt module for database dependencies
│   ├── dao/
│   │   ├── ImageDao.kt           # Image CRUD operations
│   │   ├── LayerDao.kt           # Layer management with reference counting
│   │   ├── LayerReferenceDao.kt  # Image-layer relationship management
│   │   ├── ContainerDao.kt       # Container CRUD operations
│   │   ├── RegistryDao.kt        # Registry mirror management
│   │   ├── AuthTokenDao.kt       # Registry authentication tokens
│   │   └── SearchRecordDao.kt    # Search history storage
│   └── model/
│       ├── ImageEntity.kt        # Image metadata with layer references
│       ├── LayerEntity.kt        # Layer metadata (digest, size, mediaType)
│       ├── LayerReferenceEntity.kt  # Many-to-many image-layer relationship
│       ├── ContainerEntity.kt    # Container metadata (NO status field)
│       ├── RegistryEntity.kt     # Registry mirror configuration
│       ├── RegistryType.kt       # Enum: Official, BuiltinMirror, CustomMirror
│       ├── AuthTokenEntity.kt    # Bearer tokens for registries
│       └── SearchRecordEntity.kt # Docker Hub search history
│
├── engine/
│   ├── PRootEngine.kt            # PRoot command builder and execution
│   ├── PRootEnvironment.kt       # PRoot environment setup
│   └── PRootVersion.kt           # PRoot version detection
│
├── images/
│   ├── ImageManager.kt           # Image repository operations (pull, delete, get)
│   ├── ImageClient.kt            # Docker Registry API client
│   ├── ImageReference.kt         # Image name parser (registry/repo:tag)
│   ├── DownloadProgress.kt       # Download progress tracking
│   ├── downloader/
│   │   ├── ImageDownloader.kt    # Download state machine orchestrator
│   │   ├── ImageDownloadState.kt # States: Downloading, Done, Error
│   │   └── ImageDownloadStateMachine.kt  # FlowRedux state machine
│   └── model/                    # Registry API models
│       ├── AuthTokenResponse.kt
│       ├── ImageManifestV2.kt
│       ├── ImageConfigResponse.kt
│       ├── LayerDescriptor.kt
│       ├── ContainerConfig.kt
│       └── ... (more models)
│
├── http/
│   ├── UnixHttp4kServer.kt       # Unix socket HTTP server (FILESYSTEM/ABSTRACT)
│   ├── TcpHttp4kServer.kt        # TCP HTTP server (for debugging)
│   ├── HttpProcessor.kt          # HTTP/1.1 protocol processor
│   ├── UnixClientConnection.kt   # LocalSocket client wrapper
│   ├── TcpClientConnection.kt    # TCP socket client wrapper
│   ├── ClientConnection.kt       # Connection abstraction interface
│   ├── UnixServerConfig.kt       # Unix socket configuration
│   └── TcpServerConfig.kt        # TCP server configuration
│
├── server/
│   ├── DockerApiServer.kt        # Main Docker API server (combines all routes)
│   ├── ApiModule.kt              # Hilt module for API routes
│   └── routes/                   # Docker API v1.45 route modules (Hilt multibinding)
│       ├── ContainerRoutes.kt    # /containers endpoints
│       ├── ImageRoutes.kt        # /images endpoints
│       ├── SystemRoutes.kt       # /info, /version, /ping endpoints
│       ├── VolumeRoutes.kt       # /volumes endpoints
│       ├── ExecRoutes.kt         # /exec endpoints
│       ├── NetworkRoutes.kt      # /networks endpoints
│       ├── ConfigRoutes.kt       # /configs endpoints
│       ├── DistributionRoutes.kt # /distribution endpoints
│       ├── NodeRoutes.kt         # /nodes endpoints
│       ├── PluginRoutes.kt       # /plugins endpoints
│       ├── SecretRoutes.kt       # /secrets endpoints
│       ├── ServiceRoutes.kt      # /services endpoints
│       ├── SwarmRoutes.kt        # /swarm endpoints
│       └── TaskRoutes.kt         # /tasks endpoints
│
├── registries/
│   ├── Registry.kt               # Registry instance with FlowRedux state machine
│   ├── RegistryManager.kt        # Registry tracking and health checks
│   ├── RegistryModule.kt         # Hilt module
│   ├── RegistryOperation.kt      # Operations: Check, Remove
│   ├── RegistryState.kt          # States: Healthy, Unhealthy, Checking, Removed
│   └── RegistryStateMachine.kt   # FlowRedux state machine for health checks
│
├── search/
│   ├── SearchPagingSource.kt     # Paging 3 source with URL-based pagination
│   ├── SearchRepository.kt       # Search repository exposing PagingData
│   ├── SearchHistory.kt          # DataStore-based search history (max 20 items)
│   ├── SearchParameters.kt       # Search query parameters
│   └── model/
│       ├── SearchResponse.kt     # Docker Hub search API response
│       └── SearchResult.kt       # Individual search result
│
├── io/
│   ├── File.kt                   # Symlink handling, chmod, SHA256 calculation
│   ├── Zip.kt                    # Tar/GZ extraction with symlink preservation
│   └── Tailer.kt                 # File tailing for log streaming
│
├── os/
│   ├── ProcessLimitCompat.kt     # Phantom Process Killer management (Shizuku)
│   ├── RemoteProcess.kt          # Shizuku remote process wrapper
│   ├── RemoteProcessBuilder.kt   # Shizuku process builder
│   ├── RemoteProcessBuilderService.kt  # Shizuku service implementation (AIDL)
│   ├── RemoteProcessSession.kt   # Shizuku session management
│   ├── OsModule.kt               # Hilt module for OS utilities
│   ├── JobProcess.kt             # Process wrapper with Job tracking
│   ├── Process.kt                # Process execution wrapper
│   ├── ProcessAwaiter.kt         # Process exit code awaiter
│   └── ProcessLocator.kt         # Process PID locator
│
├── logging/
│   ├── LoggingModule.kt          # Hilt module for logging
│   ├── TimberLogger.kt           # Timber-based SLF4J logger
│   └── TimberServiceProvider.kt  # SLF4J service provider (AutoService)
│
└── utils/
    ├── InState.kt                # State machine helper for state checks
    └── SuspendLazy.kt            # Lazy initialization for suspend functions
```

**AIDL Files** (for Shizuku integration):
```
daemon/src/main/aidl/com/github/andock/daemon/os/
├── IRemoteProcessBuilderService.aidl
└── IRemoteProcessSession.aidl
```

### Data Flow

1. **Image Pull Flow:**
   - `MainViewModel.pullImage()` → `ImageRepository.pullImage()`
   - `DockerRegistryApi` fetches manifest and layers
   - Each layer downloaded to `layersDir/{digest}.tar.gz` (compressed)
   - **Layers are kept compressed** to save storage space (~70% savings)
   - `ImageEntity` saved to Room database with layer references

2. **Container Creation Flow:**
   - `MainViewModel.createContainer()` → `ContainerManager.createContainer()`
   - Creates directory: `containersDir/{containerId}/ROOT/`
   - **Layers extracted directly from compressed files** to container rootfs
   - `extractTarGz()` handles symlinks, permissions, and whiteout files during extraction
   - `ContainerEntity` saved to Room database (NO status field)
   - `Container` instance created with initial state (Created or Exited)

3. **Container Execution Flow:**
   - `MainViewModel.startContainer()` → `Container.start()`
   - `Container` dispatches `Start` operation to `ContainerStateMachine`
   - State machine transitions from `Created/Exited` → `Starting` → `Running`
   - `PRootEngine` launches the container process
   - Runtime status tracked via `Container.state` StateFlow
   - Processes managed through `ContainerProcess` wrapper

### Critical Architecture Details

#### Container Status Management

**IMPORTANT:** Container status is NOT stored in the database.

- **Database (`ContainerEntity`)**: Only stores static configuration (name, imageId, config, created timestamp)
- **Runtime State (`Container`)**: Each `Container` instance has a state machine tracking current state
- **UI Layer**: Directly uses `ContainerState` class names for display (no intermediate enum)

**Container State Machine:**

The `ContainerStateMachine` manages container lifecycle with these states:
- `Created` - Container created but never started
- `Starting` - Container is being started
- `Running` - Container is actively running with main process
- `Stopping` - Container is being stopped
- `Exited` - Container has exited normally
- `Dead` - Container process died unexpectedly
- `Removing` - Container is being removed
- `Removed` - Container has been deleted

**Real-time State Observation:**
```kotlin
// In Composable - observe container state in real-time
val containerState by container.state.collectAsState()

// Use state directly for UI logic
when (containerState) {
    is ContainerState.Running -> {
        // Show stop button, terminal button
    }
    is ContainerState.Created,
    is ContainerState.Starting -> {
        // Show start button
    }
    else -> {
        // Show start button, delete button
    }
}

// Display state name
Text(text = containerState::class.simpleName ?: "Unknown")
```

**Container API:**
```kotlin
// Access container info from database
container.getInfo() // Returns Result<ContainerEntity>

// Control container lifecycle
container.start()   // Start the container
container.stop()    // Stop the container
container.remove()  // Remove the container

// Execute commands in running container
container.exec(listOf("/bin/sh", "-c", "ls -la"))  // Returns Result<ContainerProcess>
```

**Why this design:**
- Containers maintain their own state through a state machine
- State transitions are explicit and type-safe
- Prevents stale status in database (e.g., app killed while container "running")
- Single source of truth: `Container.state` tracks actual process state
- State is observable via StateFlow for reactive UI updates
- UI displays actual state names directly (no lossy enum mapping)
- Real-time updates: UI automatically recomposes when container state changes

#### PRoot Execution & SELinux

**CRITICAL:** On Android 10+ (API 29+), SELinux prevents execution of binaries from app data directories (`app_data_file` context has `execute_no_trans` denial).

**Solution:**
- PRoot compiled as `libproot.so` and packaged in APK's `jniLibs/` directory
- Android extracts it to `nativeLibraryDir` with `apk_data_file` SELinux context (executable)
- `PRootEngine` executes directly from `applicationInfo.nativeLibraryDir`
- **Never copy PRoot to app files directory** - it will become non-executable

See `PRootEngine.kt` for implementation details.

#### Layer Storage Strategy

**Design Decision:** Store compressed layers only, extract on-demand during container creation.

**Storage Structure:**
```
layersDir/
└── {sha256-digest}.tar.gz  # Compressed layer (2-5 MB)

containersDir/
└── {containerId}/
    └── ROOT/               # Extracted container filesystem (7-15 MB)
```

**Benefits:**
- **70% storage savings** (Alpine: 3MB compressed vs 10MB if both stored)
- Faster image pulls (no extraction during pull)
- Simpler layer management (no deduplication needed)

**Trade-off:** Container creation is slower due to extraction, but happens only once per container.

#### Symlink Handling

Docker images (especially Alpine Linux) rely heavily on symlinks. Standard Java `Files` API doesn't preserve them.

**Solution:** Use Android `Os` API in `utils/File.kt` during tar extraction:
- `Os.lstat()` + `OsConstants.S_ISLNK()` - detect symlinks
- `Os.readlink()` - read link target
- `Os.symlink()` - create symlink
- `Os.chmod()` - preserve permissions

All symlink/permission handling is done by `extractTarGz()` during container creation.

#### Unix Domain Socket HTTP Server

**UnixHttp4kServer** (`daemon/http/UnixHttp4kServer.kt`) provides a Unix domain socket-based HTTP server using Android's `LocalServerSocket`.

**Key Features:**
- **Dual Namespace Support**: Supports both `FILESYSTEM` and `ABSTRACT` namespaces
- **http4k Integration**: Implements `Http4kServer` interface for seamless http4k integration
- **Coroutine-based**: Uses Kotlin coroutines for concurrent request handling
- **HTTP/1.1 Protocol**: Full HTTP/1.1 support via `HttpProcessor`

**Implementation Details:**

```kotlin
// Create server with FILESYSTEM namespace (creates socket file)
val server = UnixHttp4kServer(
    name = "/data/data/com.github.adocker/files/docker.sock",
    namespace = Namespace.FILESYSTEM,
    httpHandler = myHandler
)
server.start()

// Or use ABSTRACT namespace (no file, memory-only)
val server = UnixHttp4kServer(
    name = "docker-api",
    namespace = Namespace.ABSTRACT,
    httpHandler = myHandler
)
```

**Technical Approach:**

The implementation uses a clever workaround to support both namespaces:

```kotlin
// 1. Create LocalSocket and bind to desired namespace
val localSocket = LocalSocket(LocalSocket.SOCKET_STREAM)
localSocket.bind(LocalSocketAddress(name, namespace))

// 2. Create LocalServerSocket from FileDescriptor
val serverSocket = LocalServerSocket(localSocket.fileDescriptor)
```

This bypasses `LocalServerSocket(String)`'s limitation of only supporting FILESYSTEM namespace.

**Namespace Comparison:**

| Feature | FILESYSTEM | ABSTRACT |
|---------|-----------|----------|
| **File Creation** | ✅ Creates socket file | ❌ No file (memory-only) |
| **Path** | Absolute or relative path | Simple name |
| **Visibility** | `ls`, `stat` can see file | Not visible in filesystem |
| **Cleanup** | ⚠️ Must delete manually | ✅ Auto-cleaned on close |
| **Permissions** | Subject to file permissions | N/A |
| **Use Case** | Debugging, CLI tools | App-internal IPC |

**FILESYSTEM Socket Management:**

When using `Namespace.FILESYSTEM`, the server automatically:
1. **Deletes old socket file** before starting (line 51-56)
2. **Creates new socket file** at specified path
3. **Should delete on stop** (currently missing - see note below)

```kotlin
// On start (current implementation)
if (namespace == Namespace.FILESYSTEM) {
    val socketFile = File(name)
    if (socketFile.exists() && !socketFile.delete()) {
        throw IOException("Failed to delete old socket file")
    }
}

// On stop (recommended addition)
if (namespace == Namespace.FILESYSTEM) {
    File(name).delete()
}
```

**Docker API Server Integration:**

`DockerApiServer` uses `UnixHttp4kServer` to expose Docker-compatible REST API:

```kotlin
@Singleton
class DockerApiServer @Inject constructor(
    private val appConfig: AppConfig,
    @AllRoutes private val routes: Set<@JvmSuppressWildcards RoutingHttpHandler>
) {
    private val server = UnixHttp4kServer(
        name = File(appConfig.filesDir, "docker.sock").absolutePath,
        namespace = Namespace.FILESYSTEM,
        httpHandler = routes.reduce { acc, handler -> acc.then(handler) }
    )
}
```

**Route Modules:**

All route modules in `daemon/api/routes/` use Hilt multibinding:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object ContainerRoutes {
    @IntoSet
    @Provides
    fun routes(): RoutingHttpHandler = routes(
        "/containers/json" bind GET to { Response(NOT_IMPLEMENTED) },
        // ... more routes
    )
}
```

Routes are automatically collected into `Set<RoutingHttpHandler>` and combined.

**Important Notes:**
- Socket files created by FILESYSTEM namespace must be cleaned up manually
- Default `LocalServerSocket(String)` always uses FILESYSTEM namespace
- ABSTRACT namespace is Android-specific (Linux abstract namespace sockets)
- File permissions apply to FILESYSTEM sockets but not ABSTRACT

#### Dependency Injection Pattern

All major components use constructor injection with Hilt:
```kotlin
@Singleton
class ImageRepository @Inject constructor(
    private val imageDao: ImageDao,
    private val layerDao: LayerDao,
    private val registryApi: DockerRegistryApi,
    private val appConfig: AppConfig
)
```

**IMPORTANT:** `ContainerManager` is NEVER nullable:
```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val containerManager: ContainerManager,  // NOT nullable!
    // ...
)
```

ViewModels are annotated with `@HiltViewModel` and injected into Composables via `hiltViewModel()`.

**Container Access and Real-time State Updates:**
```kotlin
// In ViewModel - transform to list for UI
val containers: StateFlow<List<Container>> = containerManager.allContainers
    .map { it.values.toList() }
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

// In Composable - observe container list
val containers by viewModel.containers.collectAsState()

// In Composable - observe individual container state for real-time updates
@Composable
fun ContainerCard(container: Container, ...) {
    // Observe state changes in real-time
    val containerState by container.state.collectAsState()

    // UI updates automatically when state changes
    when (containerState) {
        is ContainerState.Running -> ShowStopButton()
        else -> ShowStartButton()
    }
}

// Use container API
container?.start()
container?.stop()
container?.remove()
container?.exec(command)
```

#### Database Schema

Room database (`AppDatabase`, version 2) with 4 main entities:
- `ImageEntity` - pulled images with layer references
- `LayerEntity` - layer metadata (digest, size, downloaded flag, refCount)
- `ContainerEntity` - containers with config (NO status field)
- `MirrorEntity` - registry mirror configurations

**LayerEntity fields:**
- `digest` - sha256 digest (primary key)
- `size` - layer file size in bytes
- `mediaType` - MIME type (e.g., `application/vnd.docker.image.rootfs.diff.tar.gzip`)
- `downloaded` - whether the .tar.gz file exists
- `refCount` - number of images referencing this layer

**Note:** LayerEntity does NOT have an `extracted` field. Layers are stored compressed only.

All DAOs expose `Flow<List<T>>` for reactive UI updates.

**Database Migrations:**
- v1 → v2: Removed `extracted` column from `layers` table (see `AppDatabase.MIGRATION_1_2`)

#### Performance Monitoring

PRootEngine logs its initialization time using `SystemClock.elapsedRealtimeNanos()`:
```kotlin
init {
    val startTime = SystemClock.elapsedRealtimeNanos()
    // ... initialization ...
    val elapsedMs = (SystemClock.elapsedRealtimeNanos() - startTime) / 1_000_000.0
    Timber.d("PRootEngine initialization completed in %.2fms".format(elapsedMs))
}
```

**Always use `SystemClock` for time measurements**, not `System.currentTimeMillis()` (Android best practice).

## Testing Guidelines

### Test Structure
- **UI Tests:** `app/src/androidTest/java/com/github/adocker/ui/`
- **Integration Tests:** `app/src/androidTest/java/com/github/adocker/`

### HiltTestRunner
All instrumented tests must use `HiltTestRunner` (configured in `app/build.gradle.kts`):
```kotlin
@HiltAndroidTest
class MyTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject lateinit var myDependency: MyClass

    @Before
    fun setup() {
        hiltRule.inject()
    }
}
```

### PRoot Test Requirements
Tests requiring PRoot (like `ImagePullAndRunTest`) should:
1. Check `prootEngine.isAvailable` before running
2. Use `assumeTrue()` to skip tests gracefully if PRoot unavailable
3. Log detailed diagnostics for SELinux failures

Example:
```kotlin
assumeTrue("Skipping test - PRoot not available", prootAvailable)
```

### Network Test Handling
Tests involving registry access should catch network exceptions and skip gracefully:
```kotlin
try {
    imageRepository.pullImage(imageName).collect { /* ... */ }
} catch (e: java.net.UnknownHostException) {
    assumeTrue("Skipping - network unavailable: ${e.message}", false)
}
```

## UI Design System (Material Design 3)

ADocker follows **Google's Material Design 3** specifications with a complete design system for consistency and maintainability.

### Design Tokens

**File:** `app/src/main/java/com/github/adocker/ui/theme/Dimensions.kt`

All spacing, sizes, and dimensions follow the 8dp grid system:

```kotlin
object Spacing {
    val ExtraSmall = 4.dp     // Tight spacing
    val Small = 8.dp          // Default small gaps
    val Medium = 16.dp        // Standard padding
    val Large = 24.dp         // Section spacing
    val ExtraLarge = 32.dp    // Large margins
    val Huge = 48.dp          // Extra large spacing

    // Semantic spacing
    val CardPadding = 16.dp
    val ScreenPadding = 16.dp
    val ListItemSpacing = 12.dp
    val SectionSpacing = 24.dp
}

object IconSize {
    val Small = 16.dp         // Small icons in chips
    val Medium = 24.dp        // Standard icons
    val Large = 32.dp         // Prominent icons
    val ExtraLarge = 48.dp    // Hero icons
    val Huge = 64.dp          // Empty state icons
}
```

**Usage:** Always use these constants instead of hardcoded dp values:
```kotlin
// ❌ Bad
modifier = Modifier.padding(16.dp)
Icon(modifier = Modifier.size(24.dp))

// ✅ Good
modifier = Modifier.padding(Spacing.Medium)
Icon(modifier = Modifier.size(IconSize.Medium))
```

### Theme System

**File:** `app/src/main/java/com/github/adocker/ui/theme/Theme.kt`

#### Color Roles
Uses complete Material Design 3 color system:
- **primary/onPrimary**: Main brand color (Docker blue)
- **primaryContainer/onPrimaryContainer**: Subtle primary backgrounds
- **secondary/onSecondary**: Secondary actions
- **secondaryContainer/onSecondaryContainer**: Neutral containers
- **tertiary/onTertiary**: Accent color (teal)
- **tertiaryContainer/onTertiaryContainer**: Success/active states
- **error/onError**: Error states
- **errorContainer/onErrorContainer**: Error backgrounds
- **surface/onSurface**: Default surfaces
- **surfaceVariant/onSurfaceVariant**: Subtle backgrounds
- **outline/outlineVariant**: Borders and dividers

**Semantic Usage:**
```kotlin
// Running containers - success/active
containerColor = MaterialTheme.colorScheme.tertiaryContainer
contentColor = MaterialTheme.colorScheme.onTertiaryContainer

// Created containers - neutral
containerColor = MaterialTheme.colorScheme.primaryContainer
contentColor = MaterialTheme.colorScheme.onPrimaryContainer

// Exited containers - error
containerColor = MaterialTheme.colorScheme.errorContainer
contentColor = MaterialTheme.colorScheme.onErrorContainer
```

#### Typography Scale
Complete MD3 typography system defined with proper line heights and letter spacing:
- **Display** (Large/Medium/Small): Large, attention-grabbing text
- **Headline** (Large/Medium/Small): High-emphasis text
- **Title** (Large/Medium/Small): Medium-emphasis text
- **Body** (Large/Medium/Small): Main content text
- **Label** (Large/Medium/Small): Buttons, tabs, labels

### Component Patterns

#### Icon with Background
Standard pattern for prominent icons:
```kotlin
Surface(
    shape = MaterialTheme.shapes.small,
    color = MaterialTheme.colorScheme.primaryContainer,
    modifier = Modifier.size(IconSize.Large)
) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Icon(
            imageVector = Icons.Default.Layers,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(IconSize.Medium)
        )
    }
}
```

#### Chip-Style Labels
For tags, status indicators, and metadata:
```kotlin
Surface(
    shape = MaterialTheme.shapes.extraSmall,
    color = MaterialTheme.colorScheme.secondaryContainer
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.padding(
            horizontal = Spacing.Small,
            vertical = Spacing.ExtraSmall
        )
    )
}
```

#### Button Hierarchy
- **FilledTonalButton**: Primary actions (run, start, terminal)
- **OutlinedButton**: Secondary actions (stop)
- **TextButton**: Tertiary actions (cancel, dismiss)
- **error contentColor**: Dangerous actions (delete)

```kotlin
// Primary action
FilledTonalButton(onClick = { }) {
    Icon(Icons.Default.PlayArrow, null, Modifier.size(IconSize.Small))
    Spacer(Modifier.width(Spacing.Small))
    Text("Run")
}

// Dangerous action
OutlinedButton(
    onClick = { },
    colors = ButtonDefaults.outlinedButtonColors(
        contentColor = MaterialTheme.colorScheme.error
    )
) {
    Icon(Icons.Default.Delete, null, Modifier.size(IconSize.Small))
    Spacer(Modifier.width(Spacing.Small))
    Text("Delete")
}
```

### Screen Structure

All screens follow this pattern:
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScreen(
    viewModel: MyViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Title") }) },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(Spacing.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.ListItemSpacing)
        ) {
            // Content items
        }
    }
}
```

### Empty States
All list screens should have empty state with:
- Large icon (IconSize.Huge, 40% opacity)
- Title (titleLarge)
- Description (bodyMedium)
- Call-to-action button (FilledTonalButton)

```kotlin
Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.padding(Spacing.Large)
) {
    Icon(
        imageVector = Icons.Default.Layers,
        contentDescription = null,
        modifier = Modifier.size(IconSize.Huge),
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    )
    Spacer(Modifier.height(Spacing.Large))
    Text(
        text = "No images yet",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.height(Spacing.Small))
    Text(
        text = "Pull an image to get started",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(Spacing.Large))
    FilledTonalButton(onClick = { }) {
        Icon(Icons.Default.CloudDownload, null)
        Spacer(Modifier.width(Spacing.Small))
        Text("Pull Image")
    }
}
```

### Status Color Coding
- **RUNNING**: tertiaryContainer (teal/cyan - active)
- **CREATED**: primaryContainer (blue - neutral)
- **EXITED**: errorContainer (red - stopped)

Apply consistently across:
- Container cards background
- Status indicators
- Status chips
- Icon backgrounds

## Common Development Patterns

### Adding a new Repository
1. Create interface/class in `daemon/{domain}/`
2. Add `@Singleton` and `@Inject constructor`
3. Inject DAO, API client, or other dependencies
4. Register in `AppModule` if needed (usually auto-wired by Hilt)

### Adding a new Screen
1. Create Screen composable in `app/ui/screens/{feature}/`
2. Create ViewModel in `app/ui/viewmodel/` with `@HiltViewModel`
3. Add route to `Navigation.kt`
4. Use `hiltViewModel()` in composable to inject ViewModel

### Working with Container State
```kotlin
// In ViewModel - access containers
val containers: StateFlow<List<Container>> = containerManager.allContainers
    .map { it.values.toList() }
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

// In UI - pass Container instance (no status parameter needed)
ContainerCard(container = container, ...)

// In ContainerCard - observe state in real-time
@Composable
fun ContainerCard(container: Container, ...) {
    // Observe container state for real-time updates
    val containerState by container.state.collectAsState()

    // Get container info from database
    var containerInfo by remember { mutableStateOf<ContainerEntity?>(null) }
    LaunchedEffect(container) {
        container.getInfo().onSuccess { entity ->
            containerInfo = entity
        }
    }

    // Use state directly for UI logic
    when (containerState) {
        is ContainerState.Running -> ShowRunningUI()
        is ContainerState.Created,
        is ContainerState.Starting -> ShowCreatedUI()
        else -> ShowStoppedUI()
    }

    // Display state name
    Text(text = containerState::class.simpleName ?: "Unknown")
}

// In ContainersScreen - observe all states for real-time filtering
val containers by viewModel.containers.collectAsState()

// Observe all container states to trigger recomposition
val containerStates = containers.map { container ->
    container.state.collectAsState().value
}

// Calculate counts based on current states
val statusCounts = remember(containers, containerStates) {
    val running = containers.count { it.state.value is ContainerState.Running }
    // ... calculate other counts
}
```

### Working with PRoot
- Always execute from `nativeLibraryDir`, never copy the binary
- Set `PROOT_LOADER` environment variable to loader path
- Set `PROOT_TMP_DIR` to app's writable temp directory
- Use `-0` flag for root emulation (fake root user)
- Bind essential paths: `/dev`, `/proc`, `/sys`, `/system`, `/vendor`

### Working with Layers and Images
**Storage locations:**
```kotlin
// Compressed layer files (kept after pull)
File(appConfig.layersDir, "${digest.removePrefix("sha256:")}.tar.gz")

// Container rootfs (extracted during createContainer)
File(appConfig.containersDir, "$containerId/ROOT/")
```

**Extract layers during container creation:**
```kotlin
FileInputStream(layerFile).use { fis ->
    extractTarGz(fis, rootfsDir).getOrThrow()
}
```

**Key points:**
- Use `extractTarGz()` from `daemon/utils/File.kt` for layer extraction
- Extraction automatically handles symlinks, permissions, and whiteout files
- Never manually extract layers during image pull - keep them compressed
- `Os.symlink()` is used internally by `extractTarGz()` for Android compatibility

## Registry Mirror Configuration

The app supports configurable Docker registry mirrors with Bearer token authentication. Built-in mirrors include:
- Docker Hub (default)
- DaoCloud (China)
- Xuanyuan (China)
- Aliyun (China)
- Huawei Cloud (China)

Mirrors can be imported via QR code in JSON format:
```json
{
  "name": "My Mirror",
  "url": "https://mirror.example.com",
  "bearerToken": "optional_token_here"
}
```

## Docker Hub Search (Paging 3)

The app provides Docker Hub image search with infinite scroll pagination powered by Paging 3.

### Architecture

**Data Layer** (`daemon/search/`):
- `SearchClient` - Ktor HTTP client for Docker Hub Search API with URL-based pagination support
- `SearchPagingSource` - Paging 3 source using URL-based pagination (follows `next` URLs)
- `SearchRepository` - Repository exposing `Flow<PagingData<SearchResult>>`
- `SearchHistoryManager` - DataStore-based search history (max 20 items)

**UI Layer** (`app/ui/screens/search/`):
- `SearchViewModel` - ViewModel with debounced search (400ms), filters, pull tracking
- `SearchScreen` - Compose UI with `collectAsLazyPagingItems()` (`app/ui2/screens/discover/`)

### Key Features

1. **URL-based Pagination**:
   - Initial load: `GET /v2/search/repositories/?query={q}&page_size=25`
   - Subsequent loads: `GET {next_url}` (from response)
   - Uses `String?` as PagingSource key (next URL)

2. **Real-time Search**:
   - Debounced with 400ms delay using `Flow.debounce()`
   - Automatically triggers search on query change
   - Manual search adds to history

3. **Search History**:
   - Stored in DataStore Preferences (`search_history` key)
   - Max 20 items, most recent first
   - Quick access panel with remove/clear actions

4. **Advanced Filters** (UI-side filtering):
   - Official images only toggle
   - Minimum star count (0, 10, 100, 1000+)
   - Applied after data load in UI layer

5. **Image Pull Integration**:
   - Tracks active downloads via `Map<String, ImageDownloader>`
   - Shows pull state in search results
   - Can pull directly from search without navigating away

### Usage Pattern

```kotlin
// In ViewModel
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val searchHistoryManager: SearchHistoryManager
) : ViewModel() {
    val searchResults: Flow<PagingData<SearchResult>> = _searchQuery
        .debounce(400)
        .flatMapLatest { query ->
            searchRepository.searchImages(query)
        }
        .cachedIn(viewModelScope)
}

// In Composable
@Composable
fun SearchScreen(viewModel: SearchViewModel = hiltViewModel()) {
    val searchResults = viewModel.searchResults.collectAsLazyPagingItems()

    LazyColumn {
        items(count = searchResults.itemCount) { index ->
            val result = searchResults[index]
            SearchResultCard(result = result, ...)
        }

        // Handle load states
        when (searchResults.loadState.refresh) {
            is LoadState.Loading -> ShowLoadingIndicator()
            is LoadState.Error -> ShowError()
        }
    }
}
```

### Important Notes

- **DataStore Context**: `SearchHistoryManager` requires `@ApplicationContext`
- **Paging 3 Caching**: Use `.cachedIn(viewModelScope)` to survive configuration changes
- **Filter Strategy**: Filters applied in UI (not in PagingSource) for simplicity
- **Download Tracking**: Active downloads survive screen rotation via ViewModel

## Internationalization

All UI strings must be defined in:
- `app/src/main/res/values/strings.xml` (English)
- `app/src/main/res/values-zh/strings.xml` (Chinese)

Use `stringResource(R.string.key_name)` in Composables.

## Phantom Process Killer (Android 12+)

Android 12+ kills background processes aggressively. The app integrates Shizuku to disable this:
- `PhantomProcessManager` in `daemon/os/`
- UI in `app/ui/screens/settings/PhantomProcessScreen.kt`
- Requires user to grant Shizuku permission

## Verified Images

Currently tested and working:
- **Alpine Linux** (latest) - Uses musl libc and BusyBox, fully functional

Other images are experimental and may have compatibility issues.
