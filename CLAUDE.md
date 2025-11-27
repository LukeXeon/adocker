# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ADocker is an Android application that runs Docker containers without root privileges using PRoot (user-space chroot). It's a Kotlin reimplementation of the udocker concept, designed specifically for Android with full internationalization support (Chinese/English).

**Key Technologies:**
- Kotlin + Jetpack Compose (Material Design 3)
- Hilt dependency injection
- Ktor HTTP client
- Room database
- PRoot v0.15 execution engine (from green-green-avk/proot)
- Coroutines & Flow for async/reactive programming
- CameraX + ML Kit for QR code scanning
- Shizuku for system service integration

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
adocker/
├── daemon/                  # Core business logic module (Android Library)
│   └── src/main/java/com/github/adocker/daemon/
│       ├── config/         # AppConfig - centralized app configuration
│       ├── database/       # Room database, DAOs, and entities
│       ├── di/             # Hilt dependency injection modules
│       ├── containers/     # PRootEngine, ContainerExecutor, RunningContainer
│       ├── images/         # ImageRepository - image pull/storage/deletion
│       ├── registry/       # Docker Registry API client & models
│       ├── os/             # PhantomProcessManager, OS utilities
│       ├── utils/          # File operations, extraction, execution helpers
│       ├── slf4j/          # Timber logging integration
│       └── startup/        # App initialization (App Startup library)
└── app/                     # UI module (Android Application)
    └── src/main/java/com/github/adocker/ui/
        ├── model/          # UI-layer models (ContainerStatus)
        ├── screens/        # Screen composables (Home, Images, Containers, Terminal, Settings, QRCode)
        ├── viewmodel/      # ViewModels for each screen
        ├── components/     # Reusable UI components
        ├── navigation/     # Navigation graph
        └── theme/          # Material3 theme configuration
```

### Package Structure (daemon module)

```
com.github.adocker.daemon/
├── config/
│   └── AppConfig.kt              # Centralized configuration (directories, constants)
├── database/
│   ├── AppDatabase.kt            # Room database definition
│   ├── dao/                      # DAOs (ImageDao, ContainerDao, LayerDao, MirrorDao)
│   └── model/                    # Entities (ImageEntity, ContainerEntity, LayerEntity, MirrorEntity)
├── di/
│   ├── AppModule.kt              # Hilt module (provides DB, HTTP client, etc.)
│   └── AppGlobals.kt             # Hilt EntryPoint for non-Hilt contexts
├── containers/
│   ├── PRootEngine.kt            # PRoot command builder and execution
│   ├── ContainerExecutor.kt      # Manages container lifecycle (start/stop)
│   ├── RunningContainer.kt       # Represents an active container instance
│   └── ContainerRepository.kt    # Container CRUD operations
├── images/
│   ├── ImageRepository.kt        # Image pull/delete/search
│   ├── ImageReference.kt         # Image name parser
│   └── PullProgress.kt           # Pull progress tracking
├── registry/
│   ├── DockerRegistryApi.kt      # Docker Registry V2 API client
│   ├── RegistryRepository.kt     # Mirror management & health checks
│   ├── MirrorHealthChecker.kt    # Background mirror health monitoring
│   └── model/                    # Registry API models (manifests, auth, etc.)
├── os/
│   └── PhantomProcessManager.kt  # Shizuku integration for Android 12+
└── utils/
    ├── File.kt                   # File extraction, symlink handling
    └── Process.kt                # Process management utilities
```

### Data Flow

1. **Image Pull Flow:**
   - `MainViewModel.pullImage()` → `ImageRepository.pullImage()`
   - `DockerRegistryApi` fetches manifest and layers
   - Each layer downloaded to `layersDir/{digest}.tar.gz` (compressed)
   - **Layers are kept compressed** to save storage space (~70% savings)
   - `ImageEntity` saved to Room database with layer references

2. **Container Creation Flow:**
   - `MainViewModel.createContainer()` → `ContainerRepository.createContainer()`
   - Creates directory: `containersDir/{containerId}/ROOT/`
   - **Layers extracted directly from compressed files** to container rootfs
   - `extractTarGz()` handles symlinks, permissions, and whiteout files during extraction
   - `ContainerEntity` saved to Room database (NO status field)

3. **Container Execution Flow:**
   - `MainViewModel.startContainer()` → `ContainerExecutor.startContainer()`
   - `ContainerExecutor` creates `RunningContainer` via factory
   - `RunningContainer` uses `PRootEngine.startProcess()` to launch main process
   - Runtime status tracked in-memory via `RunningContainer.isActive`

### Critical Architecture Details

#### Container Status Management

**IMPORTANT:** Container status is NOT stored in the database.

- **Database (`ContainerEntity`)**: Only stores static configuration (name, imageId, config, created timestamp)
- **Runtime State (`RunningContainer`)**: Tracks active containers in-memory
- **UI Layer (`ContainerStatus` enum)**: Maps runtime state to UI (CREATED, RUNNING, EXITED)

**How to check container status:**
```kotlin
// In ViewModel
val containerStatus = if (runningContainers.value.any { it.containerId == id && it.isActive }) {
    ContainerStatus.RUNNING
} else {
    ContainerStatus.CREATED
}
```

**Why this design:**
- Containers don't have persistent "RUNNING" state - they're either active in memory or not
- Prevents stale status in database (e.g., app killed while container "running")
- Single source of truth: `RunningContainer.isActive` checks actual process state

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

**IMPORTANT:** `ContainerExecutor` and `PRootEngine` are NEVER nullable:
```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val containerExecutor: ContainerExecutor,  // NOT nullable!
    // ...
)
```

ViewModels are annotated with `@HiltViewModel` and injected into Composables via `hiltViewModel()`.

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

### Working with Container Status
```kotlin
// In ViewModel
val runningContainers: StateFlow<List<RunningContainer>> =
    containerExecutor.getAllRunningContainers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

fun getContainerStatus(containerId: String): ContainerStatus {
    val running = runningContainers.value.find { it.containerId == containerId }
    return if (running?.isActive == true) {
        ContainerStatus.RUNNING
    } else {
        ContainerStatus.CREATED
    }
}

// In UI
val status = viewModel.getContainerStatus(container.id)
ContainerCard(container = container, status = status, ...)
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
