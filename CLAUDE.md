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
./gradlew connectedAndroidTest
./gradlew connectedDebugAndroidTest --tests com.github.adocker.ImagePullAndRunTest
```

**Test requirements:** Device/emulator with ADB, JDK 17+. Tests requiring PRoot skip if binary unavailable.

### Install and run on device
```bash
export PATH="$PATH:$HOME/Library/Android/sdk/platform-tools"
adb install app/build/outputs/apk/debug/app-debug.apk
adb logcat -d | grep -i "PRootEngine\|ImagePull\|Container"
```

### PRoot Build System
PRoot is automatically compiled from source during the build process via the `proot` module:
- **Source:** Downloads PRoot v0.15 and talloc 2.4.2 automatically
- **Architectures:** arm64-v8a, armeabi-v7a, x86_64, x86
- **Output:** `libproot.so`, `libproot_loader.so`, `libproot_loader32.so` (for 64-bit archs)
- **16KB Page Alignment:** Configured for Android 15+ compatibility

Build scripts location: `proot/src/main/cpp/scripts/`

## Architecture

### Module Structure

Multi-module architecture:
- **proot/** - PRoot native build module (Android Library)
  - `src/main/cpp/CMakeLists.txt` - CMake build configuration
  - `src/main/cpp/scripts/` - Build scripts for talloc and proot
  - Downloads and compiles PRoot v0.15 + talloc 2.4.2 from source
  - Outputs: `libproot.so`, `libproot_loader.so`, `libproot_loader32.so`

- **daemon/** - Core business logic (Android Library)
  - `app/` - AppContext, AppInitializer, AppModule
  - `containers/` - Container, ContainerManager, ContainerState, ContainerStateMachine
  - `database/` - Room DB, DAOs, entities (ImageEntity, LayerEntity, ContainerEntity, etc.)
  - `engine/` - PRootEngine, PRootEnvironment
  - `images/` - ImageManager, ImageClient, ImageReference, downloader (FlowRedux)
  - `http/` - UnixHttp4kServer, TcpHttp4kServer, HttpProcessor
  - `server/` - DockerApiServer, route modules (Hilt multibinding)
  - `registries/` - RegistryManager, RegistryStateMachine (FlowRedux)
  - `search/` - SearchPagingSource, SearchRepository (Paging 3 + DataStore)
  - `io/` - File, Zip, Tailer (symlink handling, tar extraction)
  - `os/` - ProcessLimitCompat, RemoteProcess (Shizuku integration)
  - `logging/` - TimberLogger, TimberServiceProvider (SLF4J + Timber)

- **app/** - UI module (Android Application)
  - `ui/screens/` - Screen composables by feature (containers, images, search, etc.)
  - `ui/components/` - Reusable UI components
  - `ui/theme/` - Spacing.kt, IconSize.kt (Material Design 3)

**AIDL Files** (for Shizuku): `daemon/src/main/aidl/com/github/andock/daemon/os/`

### Data Flow

1. **Image Pull:** ViewModel → ImageRepository → DockerRegistryApi → download layers to `layersDir/{digest}.tar.gz` (compressed) → save ImageEntity to Room
2. **Container Creation:** ViewModel → ContainerManager → create `containersDir/{containerId}/ROOT/` → extract layers from compressed files → save ContainerEntity to Room → create Container instance
3. **Container Execution:** ViewModel → Container.start() → ContainerStateMachine transitions → PRootEngine launches process → track state via Container.state StateFlow

### Critical Architecture Details

#### Container Status Management

**IMPORTANT:** Container status is NOT stored in the database.

- **Database (`ContainerEntity`)**: Only stores static configuration (name, imageId, config, created timestamp)
- **Runtime State (`Container`)**: Each instance has a state machine tracking current state
- **UI Layer**: Directly uses `ContainerState` class names for display

**Container State Machine states:**
- `Created`, `Starting`, `Running`, `Stopping`, `Exited`, `Dead`, `Removing`, `Removed`

**Real-time State Observation:**
```kotlin
// In Composable - observe state in real-time
val containerState by container.state.collectAsState()

when (containerState) {
    is ContainerState.Running -> ShowStopButton()
    else -> ShowStartButton()
}
```

**Container API:**
```kotlin
container.getInfo() // Returns Result<ContainerEntity>
container.start()   // Start container
container.stop()    // Stop container
container.remove()  // Remove container
container.exec(listOf("/bin/sh", "-c", "ls"))  // Returns Result<ContainerProcess>
```

**Why this design:**
- State machine ensures explicit, type-safe transitions
- Prevents stale status in database (e.g., app killed while "running")
- Single source of truth: `Container.state` tracks actual process state
- Observable via StateFlow for reactive UI updates
- Real-time updates: UI auto-recomposes on state changes

#### PRoot Execution & SELinux

**CRITICAL:** On Android 10+ (API 29+), SELinux prevents execution of binaries from app data directories.

**Solution:**
- PRoot compiled as `libproot.so` in APK's `jniLibs/` directory
- Android extracts to `nativeLibraryDir` with executable SELinux context
- `PRootEngine` executes directly from `nativeLibraryDir`
- **Never copy PRoot to app files directory** - it will become non-executable

#### Layer Storage Strategy

**Design Decision:** Store compressed layers only, extract on-demand during container creation.

**Storage:**
- `layersDir/{sha256-digest}.tar.gz` - Compressed layer (2-5 MB)
- `containersDir/{containerId}/ROOT/` - Extracted container filesystem (7-15 MB)

**Benefits:** 70% storage savings, faster pulls, simpler management
**Trade-off:** Slower container creation (extraction), but happens only once

#### Symlink and Hard Link Handling

Docker images rely heavily on symlinks and hard links. Standard Java `Files` API doesn't preserve them.

**Symlink Solution:** Use Android `Os` API in `io/File.kt`:
- `Os.lstat()` + `OsConstants.S_ISLNK()` - detect symlinks
- `Os.readlink()` - read link target
- `Os.symlink()` - create symlink
- `Os.chmod()` - preserve permissions

**Hard Link Limitation (CRITICAL):**
- **Android SELinux blocks hard link creation since Android 6.0 (Marshmallow)**
- Even in app's own data directory, `link()` returns `EACCES` (Access Denied)
- This is a security policy to prevent privilege escalation attacks
- **Solution:** Automatic fallback to file copying when hard link fails
- Trade-off: Uses more disk space, but ensures compatibility

All handled by `extractTarGz()` during container creation.

#### Unix Domain Socket HTTP Server

**UnixHttp4kServer** provides Unix socket-based HTTP server using Android's `LocalServerSocket`.

**Key Features:**
- Dual Namespace Support: `FILESYSTEM` and `ABSTRACT`
- http4k Integration for seamless routing
- Coroutine-based concurrent request handling
- Full HTTP/1.1 protocol support

**Namespace Comparison:**
- **FILESYSTEM**: Creates socket file, visible in fs, requires manual cleanup
- **ABSTRACT**: Memory-only, auto-cleaned on close, Android-specific

**Docker API Server Integration:**
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

Route modules use Hilt multibinding with `@IntoSet`.

#### Dependency Injection Pattern

All major components use constructor injection with Hilt. ViewModels use `@HiltViewModel` and inject via `hiltViewModel()`.

**IMPORTANT:** `ContainerManager` is NEVER nullable.

**Container Access:**
```kotlin
// In ViewModel
val containers: StateFlow<List<Container>> = containerManager.allContainers
    .map { it.values.toList() }
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

// In Composable
val containers by viewModel.containers.collectAsState()

// Observe individual container state
@Composable
fun ContainerCard(container: Container) {
    val containerState by container.state.collectAsState()
    // UI updates automatically when state changes
}
```

#### Database Schema

Room database (version 2) with main entities:
- `ImageEntity` - images with layer references
- `LayerEntity` - layer metadata (digest, size, mediaType, downloaded, refCount)
- `ContainerEntity` - containers with config (NO status field)
- `RegistryEntity` - registry mirror configurations

**Note:** LayerEntity does NOT have `extracted` field. Layers stored compressed only.

DAOs expose `Flow<List<T>>` for reactive UI updates.

**Migrations:** v1 → v2: Removed `extracted` column from `layers` table

#### Performance Monitoring

Always use `SystemClock.elapsedRealtimeNanos()` for time measurements, not `System.currentTimeMillis()`.

## UI Design System (Material Design 3)

Follows **Google's Material Design 3** specifications.

### Design Tokens

**Files:** `app/src/main/java/com/github/andock/ui/theme/Spacing.kt`, `IconSize.kt`

```kotlin
object Spacing {
    val ExtraSmall = 4.dp, Small = 8.dp, Medium = 16.dp
    val Large = 24.dp, ExtraLarge = 32.dp, Huge = 48.dp
    val CardPadding = 16.dp, ScreenPadding = 16.dp
    val ListItemSpacing = 12.dp, SectionSpacing = 24.dp
}

object IconSize {
    val Small = 16.dp, Medium = 24.dp, Large = 32.dp
    val ExtraLarge = 48.dp, Huge = 64.dp
}
```

**Usage:** Always use constants instead of hardcoded dp values.

### Theme System

**Color Roles:**
- **primary/onPrimary**: Main brand color
- **primaryContainer/onPrimaryContainer**: Subtle backgrounds
- **tertiaryContainer/onTertiaryContainer**: Success/active states
- **errorContainer/onErrorContainer**: Error states
- **surfaceVariant/onSurfaceVariant**: Subtle backgrounds
- **outline/outlineVariant**: Borders and dividers

**Semantic Usage:**
- RUNNING: tertiaryContainer (teal - active)
- CREATED: primaryContainer (blue - neutral)
- EXITED: errorContainer (red - stopped)

### Component Patterns

**Button Hierarchy:**
- **FilledTonalButton**: Primary actions (run, start, terminal)
- **OutlinedButton**: Secondary actions (stop)
- **TextButton**: Tertiary actions (cancel, dismiss)
- **error contentColor**: Dangerous actions (delete)

### Empty States

All list screens should have:
- Large icon (IconSize.Huge, 40% opacity)
- Title (titleLarge)
- Description (bodyMedium)
- Call-to-action button (FilledTonalButton)

```kotlin
Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Icon(Icons.Default.Layers, null,
        Modifier.size(IconSize.Huge),
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
    Spacer(Modifier.height(Spacing.Large))
    Text("No items", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(Spacing.Small))
    Text("Description", style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(Spacing.Large))
    FilledTonalButton(onClick = { }) { Text("Action") }
}
```

## Common Development Patterns

### Working with Container State
```kotlin
// In ViewModel - access containers
val containers: StateFlow<List<Container>> = containerManager.allContainers
    .map { it.values.toList() }
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

// In UI - observe container state for real-time updates
@Composable
fun ContainerCard(container: Container) {
    val containerState by container.state.collectAsState()
    var containerInfo by remember { mutableStateOf<ContainerEntity?>(null) }

    LaunchedEffect(container) {
        container.getInfo().onSuccess { containerInfo = it }
    }

    when (containerState) {
        is ContainerState.Running -> ShowRunningUI()
        is ContainerState.Created, is ContainerState.Starting -> ShowCreatedUI()
        else -> ShowStoppedUI()
    }
}
```

### Working with PRoot
- Always execute from `nativeLibraryDir`, never copy the binary
- Set `PROOT_LOADER` and `PROOT_TMP_DIR` environment variables
- Use `-0` flag for root emulation
- Bind essential paths: `/dev`, `/proc`, `/sys`, `/system`, `/vendor`

### Working with Layers and Images
**Storage locations:**
```kotlin
File(appConfig.layersDir, "${digest.removePrefix("sha256:")}.tar.gz")  // Compressed
File(appConfig.containersDir, "$containerId/ROOT/")  // Extracted
```

**Extract layers during container creation:**
```kotlin
FileInputStream(layerFile).use { fis ->
    extractTarGz(fis, rootfsDir).getOrThrow()
}
```

**Key points:**
- Use `extractTarGz()` from `daemon/io/File.kt`
- Handles symlinks, permissions, whiteout files automatically
- Never extract during image pull - keep compressed
- `Os.symlink()` used internally for Android compatibility

## Docker Hub Search (Paging 3)

Docker Hub image search with infinite scroll pagination.

**Architecture:**
- `SearchClient` - Ktor HTTP client for Docker Hub API
- `SearchPagingSource` - Paging 3 source with URL-based pagination
- `SearchRepository` - Exposes `Flow<PagingData<SearchResult>>`
- `SearchHistoryManager` - DataStore-based history (max 20 items)

**Key Features:**
1. URL-based pagination following `next` URLs
2. Debounced search (400ms delay)
3. Search history in DataStore
4. UI-side filters (official images, star count)
5. Image pull integration from search

**Usage:**
```kotlin
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel() {
    val searchResults: Flow<PagingData<SearchResult>> = _searchQuery
        .debounce(400)
        .flatMapLatest { searchRepository.searchImages(it) }
        .cachedIn(viewModelScope)
}
```

## Registry Mirror Configuration

Supports configurable Docker registry mirrors with Bearer token authentication. Built-in mirrors: Docker Hub, DaoCloud, Xuanyuan, Aliyun, Huawei Cloud.

Import via QR code in JSON:
```json
{"name": "My Mirror", "url": "https://...", "bearerToken": "..."}
```

## Internationalization

All UI strings must be in:
- `app/src/main/res/values/strings.xml` (English)
- `app/src/main/res/values-zh/strings.xml` (Chinese)

Use `stringResource(R.string.key_name)` in Composables.

## Testing Guidelines

### HiltTestRunner
All instrumented tests use `HiltTestRunner`:
```kotlin
@HiltAndroidTest
class MyTest {
    @get:Rule var hiltRule = HiltAndroidRule(this)
    @Inject lateinit var myDependency: MyClass
    @Before fun setup() { hiltRule.inject() }
}
```

### PRoot Tests
Use `assumeTrue()` to skip gracefully if PRoot unavailable.

### Network Tests
Catch network exceptions and skip gracefully with `assumeTrue()`.
