# Andock - Android Docker Container Runner

**English | [中文](README.md)**

Andock is an Android application that runs Docker containers without root privileges using PRoot (user-space chroot). It's a Kotlin reimplementation of the udocker concept, designed specifically for Android with full internationalization support.

## ✨ Key Features

- **Full Internationalization** - Automatic Chinese/English interface switching
- **Image Management** - Pull, delete, view Docker images with QR code support
- **Container Management** - Create, run, stop containers with environment configuration
- **Registry Mirrors** - Built-in China mirrors, Bearer token auth, QR code import
- **Interactive Terminal** - Full container terminal access
- **Rootless** - PRoot-based technology, no root required
- **Shizuku Integration** - Optional Shizuku support to disable Phantom Process Killer

## UI Overview

The app features a modern 5-tab bottom navigation design:

| Tab | Description |
|-----|-------------|
| **Home** | Welcome page, statistics overview, quick action shortcuts |
| **Discover** | Search Docker Hub images with pagination |
| **Containers** | Container list with status filtering (All/Created/Running/Exited) |
| **Images** | Local image management, QR scan, one-click run |
| **Settings** | Registry mirrors, background process management, storage |

## Project Architecture

Multi-module architecture with clear separation of business logic and UI:

```
andock/
├── daemon/                  # Core business logic module (Android Library)
│   ├── api/                # Docker API routes
│   ├── app/                # App configuration and modules
│   ├── containers/         # Container state machine & management
│   ├── database/           # Room database, DAOs, Entities
│   ├── http/               # Unix socket HTTP server
│   ├── images/             # Image repository & downloader
│   ├── os/                 # OS integration (PhantomProcessManager)
│   ├── registry/           # Docker Registry API client
│   ├── search/             # Docker Hub search (Paging 3)
│   ├── slf4j/              # Timber logging integration
│   ├── startup/            # App initialization
│   └── utils/              # File and process utilities
└── app/                     # UI module (Android Application)
    └── ui/
        ├── components/     # Reusable UI components
        ├── navigation/     # Navigation configuration
        ├── screens/        # Screen composables
        │   ├── containers/ # Containers management
        │   ├── home/       # Home screen
        │   ├── images/     # Images management
        │   ├── qrcode/     # QR code scanner
        │   ├── search/     # Image search
        │   ├── settings/   # Settings screen
        │   └── terminal/   # Terminal screen
        ├── theme/          # Material3 theme
        └── viewmodel/      # ViewModels
```

## Tech Stack

- **Kotlin + Jetpack Compose** - Modern UI development
- **Material Design 3** - Latest design guidelines
- **Hilt** - Dependency injection framework
- **Ktor Client** - HTTP networking
- **Room** - Local database
- **Kotlinx Serialization** - JSON serialization
- **Coroutines & Flow** - Async reactive programming
- **CameraX + ML Kit** - QR code scanning
- **Shizuku** - System service integration
- **PRoot v0.15** - Container execution engine (from [green-green-avk/proot](https://github.com/green-green-avk/proot))

## Quick Start

### Build the Project

```bash
# 1. Clone repository
git clone <repository-url>
cd andock

# 2. Open in Android Studio and sync Gradle

# 3. Build and run
./gradlew assembleDebug
```

**Requirements**:
- Android Studio Ladybug (2024.2.1) or higher
- JDK 17+
- Android SDK API 36

## Usage Guide

### 1. Configure Registry Mirror (Optional)

Navigate to **Settings → Docker Registry Mirror**:
- Select built-in mirrors (Docker Hub, DaoCloud, Xuanyuan, Aliyun, Huawei)
- Add custom mirrors (supports Bearer token auth)
- **QR Code Import**: Scan QR codes to quickly import mirror configs

**Mirror QR Code Format**:
```json
{
  "name": "My Mirror",
  "url": "https://mirror.example.com",
  "bearerToken": "optional_token_here"
}
```

### 2. Search and Pull Images

**Method 1: Discover Search**
- Go to **Discover** tab
- Enter keywords to search Docker Hub (e.g., `alpine`, `nginx`)
- Tap download button on search results to pull

**Method 2: Direct Pull**
- Go to **Images** tab → tap **+** button
- Enter full image name (e.g., `alpine:latest`)
- Tap **Pull** to download

**Method 3: QR Code Scan**
- Go to **Images** tab → tap QR scan icon
- Scan QR code containing image configuration

### 3. Run Containers

- Select image in **Images** page → tap **Run** button
- Configure container parameters (name, env vars, working dir, etc.)
- Tap **Create** to start container
- Automatically navigates to **Containers** page after creation

### 4. Manage Containers

In **Containers** page you can:
- Use status filter to view containers (All/Created/Running/Exited)
- Start or stop containers
- Open terminal to execute commands
- Delete unwanted containers

### 5. Use Terminal

- Tap **Terminal** button on running container in **Containers** page
- Enter commands in terminal interface

### 6. Phantom Process Killer (Android 12+)

On Android 12+ devices, the system may kill background processes. Use Shizuku to disable:
1. Install [Shizuku](https://shizuku.rikka.app/)
2. Start Shizuku service
3. Navigate to **Settings → Background Process Limit** to grant permission and disable

## Architecture Highlights

### Container Status Management

- **Database (ContainerEntity)**: Only stores static configuration, no runtime status
- **Runtime (Container + ContainerStateMachine)**: Each container maintains its own state machine, tracking 8 states (Created, Starting, Running, Stopping, Exited, Dead, Removing, Removed)
- **UI Layer**: Directly uses `ContainerState` class names for display, observing state changes in real-time via `StateFlow`

Benefits of this design:
- Prevents stale status in database (e.g., "RUNNING" status when app is killed)
- Real-time UI updates: UI automatically recomposes when container state changes
- Type-safe state transitions: State machine ensures valid state transitions
- Precise state representation: 8 states fully express container lifecycle with no information loss

#### Real-time State Observation

Observing container state in UI components:
```kotlin
@Composable
fun ContainerCard(container: Container) {
    // Observe state changes, UI recomposes automatically
    val containerState by container.state.collectAsState()

    // Use state directly for UI logic
    when (containerState) {
        is ContainerState.Running -> ShowStopButton()
        else -> ShowStartButton()
    }

    // Display state name
    Text(text = containerState::class.simpleName ?: "Unknown")
}
```

### SELinux Compatibility

Android 10+ prevents executing binaries from `app_data_file` directories. Solution:
- PRoot compiled as `libproot.so`, placed in APK's native library directory
- Execute directly from `applicationInfo.nativeLibraryDir` (SELinux context: `apk_data_file`, executable)
- Reference: [Termux implementation](https://github.com/termux/termux-app/issues/1072)

### Symlink Support

Uses Android `Os` API to properly handle symlinks in Docker images:
- `Os.lstat()` + `OsConstants.S_ISLNK()` detect symlinks
- `Os.readlink()` read link target
- `Os.symlink()` create symlink
- `Os.chmod()` preserve permissions

Ensures Alpine Linux and other symlink-heavy images work correctly.

## Limitations

- Rootless restrictions: Some system calls unavailable
- Network isolation: Docker networking features not supported
- Storage limits: All data stored in internal storage
- Architecture limits: Only native device architecture supported

## Verified Images

- ✅ **Alpine Linux** (latest) - musl libc + BusyBox, fully compatible
- Other images under testing...

## Acknowledgments

- [udocker](https://github.com/indigo-dc/udocker) - Original concept and reference
- [PRoot](https://proot-me.github.io/) - User-space chroot implementation
- [Termux](https://termux.dev/) - Android terminal and PRoot patches
- [Shizuku](https://shizuku.rikka.app/) - System service access framework

## License

MIT License
