# ADocker - Android Docker Container Runner

**English | [中文](README.md)**

ADocker is an Android application that runs Docker containers without root privileges using PRoot (user-space chroot). It's a Kotlin reimplementation of the udocker concept, designed specifically for Android with full internationalization support.

## ✨ Key Features

- **Full Internationalization** - Automatic Chinese/English interface switching
- **Image Management** - Pull, delete, view Docker images with QR code support
- **Container Management** - Create, run, stop containers with environment configuration
- **Registry Mirrors** - Built-in China mirrors, Bearer token auth, QR code import
- **Interactive Terminal** - Full container terminal access
- **Rootless** - PRoot-based technology, no root required
- **Shizuku Integration** - Optional Shizuku support to disable Phantom Process Killer

## Project Architecture

Multi-module architecture with clear separation of business logic and UI:

```
adocker/
├── daemon/              # Core business logic module (Android Library)
│   ├── config/         # Configuration management
│   ├── database/       # Room database, DAOs, Entities
│   ├── di/             # Dependency injection
│   ├── containers/     # Container execution & management
│   ├── images/         # Image repository
│   ├── registry/       # Docker Registry API
│   ├── os/             # OS integration (PhantomProcessManager)
│   ├── utils/          # Utilities
│   └── startup/        # App initialization
└── app/                 # UI module (Android Application)
    ├── model/          # UI-layer data models
    ├── screens/        # Screen composables
    ├── viewmodel/      # ViewModels
    ├── components/     # Reusable UI components
    ├── navigation/     # Navigation configuration
    └── theme/          # Material3 theme
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
cd adocker

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

### 2. Pull Images

**Method 1: Manual Input**
- Go to **Images** → tap **+** button (or Pull Image from Home)
- Enter image name (e.g., `alpine:latest`)
- Tap **Pull** to download

**Method 2: Search Images**
- Go to **Pull Image** → tap search icon
- Enter keywords (e.g., `nginx`)
- Select image from search results and pull

**Method 3: QR Code Scan**
- Go to **Pull Image** → tap QR code icon
- Scan QR code containing image name (e.g., `alpine:latest`)
- Auto-fill and start pulling

### 3. Run Containers

- Select image in **Images** → tap **Run** button
- Configure container parameters (name, env vars, working dir, etc.)
- Tap **Create & Run** to start

### 4. Use Terminal

- Tap **Terminal** button on running container in **Containers** page
- Enter commands in terminal interface

### 5. Phantom Process Killer (Android 12+)

On Android 12+ devices, the system may kill background processes. Use Shizuku to disable:
1. Install [Shizuku](https://shizuku.rikka.app/)
2. Start Shizuku service
3. Navigate to ADocker **Settings → Phantom Process** to grant permission and disable

## Architecture Highlights

### Container Status Management

- **Database (ContainerEntity)**: Only stores static configuration, no runtime status
- **Runtime (RunningContainer)**: Tracks active containers in memory with automatic main process monitoring
- **UI Layer (ContainerStatus)**: Maps runtime state to UI (CREATED, RUNNING, EXITED)

This design prevents stale status in database (e.g., "RUNNING" status when app is killed).

#### RunningContainer Auto-Cleanup Mechanism

`RunningContainer` automatically monitors the `mainProcess` lifecycle:
- When `mainProcess` exits, all `otherProcesses` are automatically destroyed
- After `mainProcess` exits, new processes cannot be launched via `execCommand`
- Uses background daemon thread for monitoring, no manual polling required

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
- Storage limits: Constrained by Android app sandbox
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
