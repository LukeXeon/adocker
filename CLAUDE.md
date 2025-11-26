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

### Package Structure

The codebase follows a clean architecture pattern with clear separation of concerns:

```
com.github.adocker/
├── core/                    # Core business logic and infrastructure
│   ├── config/             # AppConfig - centralized app configuration
│   ├── database/           # Room database, DAOs, and entities
│   ├── di/                 # Hilt dependency injection modules
│   ├── engine/             # PRootEngine - container execution engine
│   ├── image/              # ImageRepository - image pull/storage/deletion
│   ├── container/          # ContainerRepository & ContainerExecutor
│   ├── registry/           # Docker Registry API client & models
│   ├── process/            # Process management utilities
│   ├── utils/              # File operations, extraction, execution helpers
│   ├── logging/            # Timber logging setup
│   └── startup/            # App initialization (App Startup library)
└── ui/                      # UI layer (Jetpack Compose)
    ├── screens/            # Screen composables (Home, Images, Containers, Terminal, Settings, QRCode)
    ├── viewmodel/          # ViewModels for each screen
    ├── components/         # Reusable UI components
    ├── navigation/         # Navigation graph
    └── theme/              # Material3 theme configuration
```

### Data Flow

1. **Image Pull Flow:**
   - `MainViewModel.pullImage()` → `ImageRepository.pullImage()`
   - `DockerRegistryApi` fetches manifest and layers
   - Each layer downloaded to `layersDir/{digest}.tar.gz`
   - Extracted to `layersDir/{digest}/` with proper symlink handling
   - `ImageEntity` saved to Room database

2. **Container Creation Flow:**
   - `MainViewModel.createContainer()` → `ContainerRepository.createContainer()`
   - Creates directory: `containersDir/{containerId}/ROOT/`
   - Layers merged using overlay FS approach (copy files, symlinks preserved)
   - `ContainerEntity` saved to Room database (without status/pid - see Architecture Details)

3. **Container Execution Flow:**
   - `MainViewModel.startContainer()` → `ContainerExecutor.startContainer()`
   - `PRootEngine.buildCommand()` constructs PRoot command with binds
   - `PRootEngine.buildEnvironment()` sets `PROOT_LOADER` and environment vars
   - Process started and tracked in `ContainerExecutor.runningProcesses` map

4. **Container Status Query Flow:**
   - UI requests containers from ViewModel
   - ViewModel combines `ContainerEntity` from Repository with runtime status from Executor
   - Status is computed dynamically by checking if process exists in `runningProcesses`
   - Result wrapped in `ContainerWithStatus` for UI consumption

### Critical Architecture Details

#### Container State Management (IMPORTANT!)

**Design Principle:** Container runtime state (status, PID) is NOT stored in the database.

**Why?**
- App crashes would leave stale status in database
- Android Phantom Process Killer can terminate processes without updating database
- Matches Docker's design: status is derived, not stored

**Implementation:**
- `ContainerEntity` (database) - Only static metadata: id, name, imageId, imageName, created, config
- `ContainerExecutor` (runtime) - Sole authority for process state via `runningProcesses` map
- `ContainerWithStatus` (UI layer) - Lightweight wrapper combining entity + computed status
- Status computed by: `isContainerRunning()` checks if ID exists in `runningProcesses` map

**Layer Responsibilities:**
```
ContainerRepository  → CRUD operations, returns ContainerEntity
ContainerExecutor    → Process management, provides getContainerStatus() API
ViewModel            → Combines data + status into ContainerWithStatus
UI                   → Displays ContainerWithStatus
```

**NEVER:**
- Store status/pid in database
- Check status in Repository layer
- Duplicate runtime state across layers

#### PRoot Execution & SELinux

**CRITICAL:** On Android 10+ (API 29+), SELinux prevents execution of binaries from app data directories (`app_data_file` context has `execute_no_trans` denial).

**Solution:**
- PRoot compiled as `libproot.so` and packaged in APK's `jniLibs/` directory
- Android extracts it to `nativeLibraryDir` with `apk_data_file` SELinux context (executable)
- `PRootEngine` executes directly from `applicationInfo.nativeLibraryDir`
- **Never copy PRoot to app files directory** - it will become non-executable

See `PRootEngine.kt:26-32` for implementation details.

#### Symlink Handling

Docker images (especially Alpine Linux) rely heavily on symlinks. Standard Java `Files` API doesn't preserve them.

**Solution:** Use Android `Os` API (see `ContainerRepository.kt:80-150`):
- `Os.lstat()` + `OsConstants.S_ISLNK()` - detect symlinks
- `Os.readlink()` - read link target
- `Os.symlink()` - create symlink
- `Os.chmod()` - preserve permissions

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

ViewModels are annotated with `@HiltViewModel` and injected into Composables via `hiltViewModel()`.

#### Database Schema

Room database (`AppDatabase`) with 4 main entities:
- `ImageEntity` - pulled images with layer references
- `LayerEntity` - image layers (sha256 digests)
- `ContainerEntity` - containers with config (NO status/pid - see State Management above)
- `MirrorEntity` - registry mirror configurations

All DAOs expose `Flow<List<T>>` for reactive UI updates.

**Note:** `container.json` file was removed - it duplicated database data.

## Testing Guidelines

### Test Structure
- **UI Tests:** `app/src/androidTest/java/com/github/adocker/ui/`
- **Integration Tests:** `app/src/androidTest/java/com/github/adocker/`

### HiltTestRunner
All instrumented tests must use `HiltTestRunner` (configured in `app/build.gradle.kts:22`):
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
1. Check `prootEngine.isAvailable()` before running
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
1. Create interface/class in `core/{domain}/`
2. Add `@Singleton` and `@Inject constructor`
3. Inject DAO, API client, or other dependencies
4. Register in `AppModule` if needed (usually auto-wired by Hilt)

### Adding a new Screen
1. Create Screen composable in `ui/screens/{feature}/`
2. Create ViewModel in `ui/viewmodel/` with `@HiltViewModel`
3. Add route to `Navigation.kt`
4. Use `hiltViewModel()` in composable to inject ViewModel

### Working with PRoot
- Always execute from `nativeLibraryDir`, never copy the binary
- Set `PROOT_LOADER` environment variable to loader path
- Set `PROOT_TMP_DIR` to app's writable temp directory
- Use `-0` flag for root emulation (fake root user)
- Bind essential paths: `/dev`, `/proc`, `/sys`, `/system`, `/vendor`

### File Extraction
- Use `extractTarGz()` from `core/utils/` for layer extraction
- Use `Os.symlink()` instead of `Files.createSymbolicLink()` for Android compatibility
- Always preserve file permissions with `Os.chmod()`

## Registry Mirror Configuration

The app supports configurable Docker registry mirrors with Bearer token authentication. Built-in mirrors include:
- Docker Hub (default)
- DaoCloud (China)
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
- `PhantomProcessManager` in `core/utils/`
- UI in `screens/settings/PhantomProcessScreen.kt`
- Requires user to grant Shizuku permission

## Verified Images

Currently tested and working:
- **Alpine Linux** (latest) - Uses musl libc and BusyBox, fully functional

Other images are experimental and may have compatibility issues.
