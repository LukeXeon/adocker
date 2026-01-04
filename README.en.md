# Andock - Android Docker Container Runner

**English | [中文](README.md)**

Andock is an Android application that runs Docker containers without root privileges using PRoot (user-space chroot). It's a complete Kotlin reimplementation of the udocker concept, designed specifically for Android.

## ✨ Key Features

- **🐳 Full Container Management** - Create, start, stop, delete containers with environment variables and working directory configuration
- **📦 Image Management** - Pull images from Docker Registry, view image details, delete unwanted images
- **🔍 Docker Hub Search** - Integrated Docker Hub search with infinite scroll pagination (Paging 3)
- **🌍 Registry Mirrors** - Built-in China mirrors, custom mirror support with Bearer token authentication
- **📱 QR Code Import** - Scan QR codes using CameraX + ML Kit to quickly import registry configurations
- **💻 Interactive Terminal** - Full container terminal access with exec command support
- **🚀 Rootless** - Based on PRoot v0.15 technology, runs entirely in user-space
- **🔧 Shizuku Integration** - Optional Shizuku support to disable Android 12+ Phantom Process Killer
- **🌐 Docker API Server** - Built-in Docker Engine API v1.45 compatible HTTP server (Unix Socket)
- **🎨 Material Design 3** - Follows Google's latest design guidelines with automatic dark/light theme switching
- **🌏 Full Internationalization** - Automatic Chinese/English interface switching

## 🏗️ Project Architecture

Multi-module architecture with clean separation between business logic and UI:

```
andock/
├── daemon/                           # Core business logic module (Android Library)
│   ├── app/                          # Application configuration and initialization
│   │   ├── AppContext.kt             # Application context and directory config
│   │   ├── AppInitializer.kt         # App Startup initialization
│   │   └── AppModule.kt              # Hilt DI module
│   │
│   ├── containers/                   # Container management
│   │   ├── Container.kt              # Container instance (FlowRedux state machine)
│   │   ├── ContainerManager.kt       # Container creation, tracking, deletion
│   │   ├── ContainerState.kt         # 8 states (Created, Starting, Running...)
│   │   └── ContainerStateMachine.kt  # State machine logic
│   │
│   ├── database/                     # Room database
│   │   ├── AppDatabase.kt            # Database definition (version 1)
│   │   ├── dao/                      # Data Access Objects
│   │   │   ├── ImageDao.kt
│   │   │   ├── LayerDao.kt
│   │   │   ├── ContainerDao.kt
│   │   │   ├── RegistryDao.kt
│   │   │   ├── AuthTokenDao.kt
│   │   │   └── SearchRecordDao.kt
│   │   └── model/                    # Database entities
│   │       ├── ImageEntity.kt
│   │       ├── LayerEntity.kt
│   │       ├── LayerReferenceEntity.kt
│   │       ├── ContainerEntity.kt
│   │       ├── RegistryEntity.kt
│   │       ├── AuthTokenEntity.kt
│   │       └── SearchRecordEntity.kt
│   │
│   ├── engine/                       # PRoot execution engine
│   │   ├── PRootEngine.kt            # PRoot command builder and execution
│   │   ├── PRootEnvironment.kt       # Environment variable configuration
│   │   └── PRootVersion.kt           # Version detection
│   │
│   ├── images/                       # Image management
│   │   ├── ImageManager.kt           # Image repository operations
│   │   ├── ImageClient.kt            # Docker Registry API client
│   │   ├── ImageReference.kt         # Image name parser
│   │   ├── downloader/               # Image downloader
│   │   │   ├── ImageDownloader.kt    # Download state machine
│   │   │   └── ImageDownloadState.kt # Download states
│   │   └── model/                    # Registry API models
│   │
│   ├── http/                         # HTTP server infrastructure
│   │   ├── UnixHttp4kServer.kt       # Unix Socket HTTP server
│   │   ├── TcpHttp4kServer.kt        # TCP HTTP server (for debugging)
│   │   └── HttpProcessor.kt          # HTTP/1.1 protocol processor
│   │
│   ├── server/                       # Docker API server
│   │   ├── DockerApiServer.kt        # Main server (combines all routes)
│   │   └── routes/                   # Docker API v1.45 route modules
│   │       ├── ContainerRoutes.kt
│   │       ├── ImageRoutes.kt
│   │       ├── SystemRoutes.kt
│   │       ├── VolumeRoutes.kt
│   │       ├── ExecRoutes.kt
│   │       └── ... (more routes)
│   │
│   ├── registries/                   # Registry mirror management
│   │   ├── Registry.kt               # Registry instance (FlowRedux state machine)
│   │   ├── RegistryManager.kt        # Registry tracking and health checks
│   │   └── RegistryStateMachine.kt   # Health check state machine
│   │
│   ├── search/                       # Docker Hub search
│   │   ├── SearchPagingSource.kt     # Paging 3 data source
│   │   ├── SearchRepository.kt       # Search repository
│   │   ├── SearchHistory.kt          # DataStore search history
│   │   └── model/                    # Search result models
│   │
│   ├── io/                           # File I/O utilities
│   │   ├── File.kt                   # Symlink handling, SHA256 calculation
│   │   ├── Zip.kt                    # Tar/GZ extraction (preserves symlinks)
│   │   └── Tailer.kt                 # Log tailing
│   │
│   ├── os/                           # Operating system integration
│   │   ├── ProcessLimitCompat.kt     # Phantom Process Killer management
│   │   ├── RemoteProcess.kt          # Shizuku process wrapper
│   │   └── Process.kt                # Process execution utilities
│   │
│   └── logging/                      # Logging infrastructure
│       ├── TimberLogger.kt           # Timber + SLF4J integration
│       └── TimberServiceProvider.kt  # SLF4J service provider
│
└── app/                              # UI module (Android Application)
    ├── AndockApplication.kt          # Application class
    ├── ui/
    │   ├── MainActivity.kt           # Single Activity + Compose
    │   │
    │   ├── screens/                  # Screen composables (by feature)
    │   │   ├── main/                 # Main navigation screen
    │   │   │   └── MainScreen.kt     # Bottom navigation
    │   │   ├── home/                 # Home (dashboard)
    │   │   │   ├── HomeScreen.kt
    │   │   │   └── HomeViewModel.kt
    │   │   ├── containers/           # Container management
    │   │   │   ├── ContainersScreen.kt       # Container list
    │   │   │   ├── ContainerDetailScreen.kt  # Container details
    │   │   │   ├── ContainerCreateScreen.kt  # Create container
    │   │   │   └── ContainersViewModel.kt
    │   │   ├── images/               # Image management
    │   │   │   ├── ImagesScreen.kt
    │   │   │   ├── ImageDetailScreen.kt
    │   │   │   ├── ImagePullDialog.kt
    │   │   │   └── ImagesViewModel.kt
    │   │   ├── search/               # Docker Hub search
    │   │   │   ├── SearchScreen.kt
    │   │   │   ├── SearchFilterPanel.kt
    │   │   │   ├── SearchHistoryPanel.kt
    │   │   │   └── SearchViewModel.kt
    │   │   ├── registries/           # Registry mirror management
    │   │   │   ├── RegistriesScreen.kt
    │   │   │   ├── AddMirrorScreen.kt
    │   │   │   └── RegistriesViewModel.kt
    │   │   ├── qrcode/               # QR code scanner
    │   │   │   ├── QrcodeScannerScreen.kt
    │   │   │   └── QrcodeCamera.kt
    │   │   ├── terminal/             # Container terminal
    │   │   │   ├── TerminalScreen.kt
    │   │   │   └── TerminalViewModel.kt
    │   │   ├── limits/               # Process limit management
    │   │   │   └── ProcessLimitScreen.kt
    │   │   └── settings/             # Settings screen
    │   │       ├── SettingsScreen.kt
    │   │       └── SettingsViewModel.kt
    │   │
    │   ├── components/               # Reusable UI components
    │   │   ├── DetailCard.kt
    │   │   ├── StatusIndicator.kt
    │   │   ├── LoadingDialog.kt
    │   │   └── ...
    │   │
    │   └── theme/                    # Material Design 3 theme
    │       ├── Spacing.kt            # Spacing constants (8dp grid)
    │       └── IconSize.kt           # Icon size constants
    │
    └── src/main/jniLibs/             # Native libraries
        ├── arm64-v8a/
        │   ├── libproot.so           # PRoot executable
        │   ├── libproot_loader.so    # 64-bit loader
        │   └── libproot_loader32.so  # 32-bit loader
        ├── armeabi-v7a/
        ├── x86_64/
        └── x86/
```

## 🔧 Tech Stack

### Core Frameworks
- **Kotlin 2.2.21** - 100% Kotlin codebase
- **Jetpack Compose** - Modern declarative UI (BOM 2025.12.00)
- **Material Design 3** - Google's latest design guidelines
- **Coroutines & Flow 1.10.2** - Async reactive programming

### Dependency Injection
- **Hilt 2.57.2** - Dependency injection framework
- **AssistedInject** - Dynamic instance creation (Container, ImageDownloader, Registry)

### Data Layer
- **Room 2.8.4** - Local SQLite database
- **DataStore** - Lightweight key-value storage (search history)
- **Paging 3.3.5** - Pagination loading (Docker Hub search)

### Networking
- **Ktor 3.3.3** - HTTP client (OkHttp engine)
- **kotlinx-serialization 1.9.0** - JSON serialization
- **http4k 6.23.1.0** - HTTP server framework

### State Management
- **FlowRedux 2.0.0** - State machine framework (containers, downloads, registries)

### File Processing
- **Apache Commons Compress 1.28.0** - Tar/GZ extraction
- **XZ 1.11** - .tar.xz format support

### Image Loading
- **Coil 3.3.0** - Image loading library (Compose integration)

### Camera & QR Scanning
- **CameraX 1.5.2** - Camera API
- **ML Kit Barcode Scanning 17.3.0** - QR code recognition

### System Integration
- **Shizuku 13.1.5** - System permission management
- **PRoot v0.15** - User-space chroot (from [green-green-avk/proot](https://github.com/green-green-avk/proot))

### Logging
- **Timber 5.0.1** - Android logging library
- **SLF4J 2.0.17** - Unified logging interface

### Navigation
- **Navigation Compose 2.9.6** - Type-safe navigation

## 🚀 Quick Start

### Requirements

- **Android Studio** Ladybug (2024.2.1) or higher
- **JDK 17+**
- **Android SDK API 36**
- **Device Requirements**: Android 8.0+ (API 26+)

### Build the Project

```bash
# 1. Clone repository
git clone <repository-url>
cd andock

# 2. Open in Android Studio and sync Gradle

# 3. Build Debug APK
./gradlew assembleDebug

# 4. Install to device (requires ADB connection)
export PATH="$PATH:$HOME/Library/Android/sdk/platform-tools"
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Run Tests

```bash
# Setup environment variables
export PATH="$PATH:$HOME/Library/Android/sdk/platform-tools"
export JAVA_HOME=/opt/homebrew/opt/openjdk@17  # macOS

# Run all tests
./gradlew connectedAndroidTest

# Run specific test
./gradlew connectedDebugAndroidTest --tests com.github.andock.ImagePullAndRunTest
```

## 📖 Usage Guide

### 1. Configure Registry Mirror (Optional but Recommended)

**Steps:**
1. Open the app, tap **Registries** tab
2. Select built-in mirrors or add custom mirrors

**Built-in Mirrors:**
- **Docker Hub** (Official) - registry-1.docker.io
- **DaoCloud** (China) - docker.m.daocloud.io
- **Xuanyuan** (China) - docker.xuanyuan.me
- **Huawei Cloud** (China) - mirrors.huaweicloud.com

**QR Code Import Format:**
```json
{
  "name": "My Custom Mirror",
  "url": "https://mirror.example.com",
  "bearerToken": "optional_token_here"
}
```

### 2. Search and Pull Images

**Method 1: Docker Hub Search**
1. Tap **Search** tab
2. Enter keywords (e.g., `alpine`, `nginx`, `ubuntu`)
3. Tap download button on search results

**Method 2: Direct Pull**
1. Tap **Images** tab → tap **+** button
2. Enter full image name (e.g., `alpine:latest`)
3. Tap **Pull** to start download

**Method 3: QR Code Scan**
1. Tap **Images** tab → tap QR scan icon
2. Scan QR code containing image configuration

### 3. Run Containers

**Steps:**
1. Select image in **Images** page → tap **Run** button
2. Configure container parameters:
   - **Container Name** (auto-generated or custom)
   - **Environment Variables** (e.g., `KEY=VALUE`)
   - **Working Directory** (optional)
   - **Command** (override default CMD, optional)
3. Tap **Create** to create and start container

### 4. Manage Containers

In **Containers** page you can:
- Use filters to view containers (All/Running/Exited)
- Start or stop containers
- View container details (environment variables, ports, network, etc.)
- Open terminal to execute commands
- Delete unwanted containers

**Container States:**
- **Created** - Created but not started
- **Starting** - Starting in progress
- **Running** - Running
- **Stopping** - Stopping in progress
- **Exited** - Exited normally
- **Dead** - Process died unexpectedly
- **Removing** - Being removed
- **Removed** - Deleted

### 5. Use Terminal

**Steps:**
1. Tap **Terminal** button on running container in **Containers** page
2. Enter commands in terminal interface
3. Quick command buttons supported (e.g., `ls`, `pwd`, `ps`)

### 6. Disable Phantom Process Killer (Android 12+)

On Android 12+ devices, the system may kill background processes. Use Shizuku to disable this restriction:

**Steps:**
1. Install [Shizuku](https://shizuku.rikka.app/)
2. Start Shizuku service
3. Open app, go to **Settings** → **Background Process Limit**
4. Grant Shizuku permission
5. Tap **Disable Limit** button

**Implementation:**
```bash
# Android 12L+ (API 32+)
settings put global settings_enable_monitor_phantom_procs false

# Android 12 (API 31)
device_config set_sync_disabled_for_tests persistent
device_config put activity_manager max_phantom_processes 2147483647
```

## 🏛️ Architecture Highlights

### 1. Container State Management (FlowRedux State Machine)

**Design Decision: Database Does NOT Store Runtime Status**

- **Database (ContainerEntity)**: Only stores static configuration (name, image, created time, config)
- **Runtime (Container)**: Each container instance maintains its own state machine, tracking 8 states
- **UI Layer**: Directly observes `Container.state` StateFlow for real-time updates

**State Transitions:**
```
Created → Starting → Running → Stopping → Exited → Removing → Removed
                        ↓
                     Dead
```

**Benefits:**
- Prevents stale status in database (e.g., "Running" status becomes invalid when app is killed)
- Real-time UI updates: UI automatically recomposes when state changes
- Type-safe state transitions: State machine ensures valid transitions
- Precise state representation: 8 states fully express container lifecycle

**Example Code:**
```kotlin
@Composable
fun ContainerCard(container: Container) {
    // Observe state changes, UI recomposes automatically
    val containerState by container.state.collectAsState()

    // Use state directly for UI logic
    when (containerState) {
        is ContainerState.Running -> ShowRunningUI()
        is ContainerState.Created -> ShowCreatedUI()
        is ContainerState.Exited -> ShowExitedUI()
        else -> ShowDefaultUI()
    }

    // Display state name
    Text(text = containerState::class.simpleName ?: "Unknown")
}
```

### 2. Layer Storage Strategy (Compression-First)

**Design Decision: Store Compressed Layers Only, Extract On-Demand**

**Storage Structure:**
```
layersDir/
└── {sha256-digest}.tar.gz    # Compressed layer (2-5 MB)

containersDir/
└── {containerId}/
    └── rootfs/               # Extracted container filesystem (7-15 MB)
```

**Benefits:**
- **70% storage savings** (Alpine: 3MB compressed vs 10MB extracted)
- Faster image pulls (no extraction needed)
- Simpler layer management (no deduplication needed)

**Trade-off:** Container creation is slower due to extraction, but this happens only once per container

### 3. SELinux Compatibility (Android 10+)

**Problem:** Android 10+ prevents executing binaries from `app_data_file` directories

**Solution:**
1. PRoot compiled as `libproot.so`, placed in APK's `jniLibs/` directory
2. Android automatically extracts to `nativeLibraryDir`, SELinux context is `apk_data_file` (executable)
3. Execute directly from `applicationInfo.nativeLibraryDir`

**Reference:** [Termux implementation](https://github.com/termux/termux-app/issues/1072)

### 4. Symlink Support (Os API)

Docker images (especially Alpine Linux) heavily rely on symlinks. Standard Java `Files` API doesn't preserve them.

**Solution:** Use Android `Os` API to handle symlinks
```kotlin
- Os.lstat() + OsConstants.S_ISLNK() // Detect symlinks
- Os.readlink()                      // Read link target
- Os.symlink()                       // Create symlink
- Os.chmod()                         // Preserve permissions
```

Ensures Alpine Linux and other symlink-heavy images work correctly.

### 5. Unix Socket HTTP Server (http4k)

**Implementation:** `UnixHttp4kServer` supports FILESYSTEM and ABSTRACT namespaces

**Namespace Comparison:**

| Feature | FILESYSTEM | ABSTRACT |
|---------|-----------|----------|
| **File Creation** | ✅ Creates socket file | ❌ No file (memory-only) |
| **Visibility** | `ls`, `stat` can see | Not visible in filesystem |
| **Cleanup** | ⚠️ Must delete manually | ✅ Auto-cleaned on close |
| **Permissions** | Subject to file permissions | N/A |
| **Use Case** | Debugging, CLI tools | App-internal IPC |

**Docker API Integration:**
```kotlin
UnixHttp4kServer(
    name = File(appConfig.filesDir, "docker.sock").absolutePath,
    namespace = Namespace.FILESYSTEM,
    httpHandler = routes.reduce { acc, handler -> acc.then(handler) }
)
```

### 6. Docker Hub Search (Paging 3)

**URL-based Pagination:**
- **PagingSource Key:** URL (String?)
- **Initial Request:** `/v2/search/repositories/?query={q}&page_size=25`
- **Subsequent Requests:** Follow `next` URL from response

**Features:**
- Debounced search (400ms delay)
- Search history (DataStore, max 20 items)
- UI-side filtering (official images, minimum stars)
- Real-time pull state tracking

## ⚠️ Limitations

- **No root privileges**: Some system calls unavailable (e.g., `mount`, `chroot`)
- **Network isolation**: Docker networking features not supported (bridge, overlay)
- **Storage limits**: All data stored in app's internal storage
- **Architecture limits**: Only native device architecture supported (no emulation)
- **Performance**: PRoot introduces approximately 10-15% performance overhead

## ✅ Verified Images

- ✅ **Alpine Linux** (latest) - Uses musl libc and BusyBox, fully compatible
- ✅ **BusyBox** (latest) - Minimal toolset, fully compatible
- ⚠️ **Ubuntu** (requires large storage, some features limited)
- ⚠️ **Nginx** (affected by network limitations)
- Other images under testing...

## 📂 Data Directories

```
/data/data/com.github.andock/
├── files/
│   ├── containers/
│   │   └── {containerId}/
│   │       └── rootfs/           # Container filesystem
│   └── layers/
│       └── {digest}.tar.gz       # Compressed layer files
├── cache/
│   ├── log/
│   │   └── {containerId}/
│   │       ├── stdout            # Container stdout
│   │       └── stderr            # Container stderr
│   └── docker.sock               # Docker API Unix Socket
└── databases/
    └── andock.db                # Room database
```

## 🙏 Acknowledgments

- [udocker](https://github.com/indigo-dc/udocker) - Original concept and reference implementation
- [PRoot](https://proot-me.github.io/) - User-space chroot implementation
- [Termux](https://termux.dev/) - Android terminal and PRoot patches
- [Shizuku](https://shizuku.rikka.app/) - System service access framework
- [green-green-avk/proot](https://github.com/green-green-avk/proot) - PRoot v0.15 for Android

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
