# ADocker - Android Docker Container Runner

ADocker 是一个在 Android 上运行 Docker 容器的应用，基于 udocker 概念用 Kotlin 重新实现，使用 PRoot 作为执行引擎。

[English](#english) | [中文](#chinese)

## <a id="chinese"></a>中文

### ✨ 核心特性

- **完整国际化支持** - 中英文界面自动切换
- **镜像管理** - 拉取、删除、查看 Docker 镜像，支持二维码扫描
- **容器管理** - 创建、运行、停止容器，支持环境变量配置
- **镜像加速** - 内置中国镜像源，支持 Bearer Token 认证，二维码导入配置
- **交互终端** - 完整的容器终端访问
- **无需 Root** - 基于 PRoot 技术，无需 root 权限
- **Shizuku 集成** - 可选使用 Shizuku 禁用 Phantom Process Killer，提升稳定性

### 项目架构

项目采用多模块架构，清晰分离业务逻辑和UI层：

```
adocker/
├── daemon/              # 核心业务逻辑模块 (Android Library)
│   ├── config/         # 配置管理
│   ├── database/       # Room数据库、DAO、Entity
│   ├── di/             # 依赖注入
│   ├── containers/     # 容器执行和管理
│   ├── images/         # 镜像仓库
│   ├── registry/       # Docker Registry API
│   ├── os/             # 系统集成 (PhantomProcessManager)
│   ├── utils/          # 工具类
│   └── startup/        # 应用初始化
└── app/                 # UI模块 (Android Application)
    ├── model/          # UI层数据模型
    ├── screens/        # 页面
    ├── viewmodel/      # ViewModel层
    ├── components/     # 通用UI组件
    ├── navigation/     # 导航配置
    └── theme/          # Material3 主题
```

### 技术栈

- **Kotlin + Jetpack Compose** - 现代化 UI 开发
- **Material Design 3** - 遵循最新设计规范
- **Hilt** - 依赖注入框架
- **Ktor Client** - HTTP 网络通信
- **Room** - 本地数据库
- **Kotlinx Serialization** - JSON 序列化
- **Coroutines & Flow** - 异步响应式编程
- **CameraX + ML Kit** - 二维码扫描
- **Shizuku** - 系统服务集成
- **PRoot v0.15** - 容器执行引擎（来自 [green-green-avk/proot](https://github.com/green-green-avk/proot)）

### 快速开始

#### 编译项目

```bash
# 1. 克隆项目
git clone <repository-url>
cd adocker

# 2. 使用 Android Studio 打开并同步 Gradle

# 3. 编译运行
./gradlew assembleDebug
```

**环境要求**:
- Android Studio Ladybug (2024.2.1) 或更高版本
- JDK 17+
- Android SDK API 36

### 使用说明

#### 1. 配置镜像加速（可选）

进入 **Settings → Docker Registry Mirror**:
- 选择内置镜像源（Docker Hub, DaoCloud, Xuanyuan, Aliyun, Huawei）
- 添加自定义镜像源（支持 Bearer Token 认证）
- **二维码导入**：扫描二维码快速导入镜像配置

**镜像源 QR 码格式**:
```json
{
  "name": "My Mirror",
  "url": "https://mirror.example.com",
  "bearerToken": "optional_token_here"
}
```

#### 2. 拉取镜像

**方式一：手动输入**
- 进入 **Images** 页面 → 点击 **+** 按钮（或从主页进入 Pull Image）
- 输入镜像名称（如 `alpine:latest`）
- 点击 **Pull** 开始下载

**方式二：搜索镜像**
- 进入 **Pull Image** 页面 → 点击搜索图标
- 在搜索页面输入关键词（如 `nginx`）
- 从搜索结果中选择镜像并拉取

**方式三：扫描二维码**
- 进入 **Pull Image** 页面 → 点击二维码图标
- 扫描包含镜像名称的二维码（如 `alpine:latest`）
- 自动填充并开始拉取

#### 3. 运行容器

- 在 **Images** 页面选择镜像 → 点击 **Run** 按钮
- 配置容器参数（名称、环境变量、工作目录等）
- 点击 **Create & Run** 启动容器

#### 4. 使用终端

- 在 **Containers** 页面点击运行中容器的 **Terminal** 按钮
- 在终端界面输入命令并执行

#### 5. Phantom Process Killer（Android 12+）

在 Android 12+ 设备上，系统可能会杀死后台进程。使用 Shizuku 可以禁用此限制：
1. 安装 [Shizuku](https://shizuku.rikka.app/)
2. 启动 Shizuku 服务
3. 进入 ADocker **Settings → Phantom Process** 授权并禁用 Killer

### 架构亮点

#### 容器状态管理

- **数据库（ContainerEntity）**：只存储静态配置，不存储运行状态
- **运行时（RunningContainer）**：内存中追踪活跃容器
- **UI层（ContainerStatus）**：映射运行状态到UI显示（CREATED, RUNNING, EXITED）

这种设计避免了数据库中状态过期的问题（如应用被杀死时容器"RUNNING"状态会失效）。

#### SELinux 兼容

Android 10+ 禁止从 `app_data_file` 目录执行二进制文件。解决方案：
- PRoot 编译为 `libproot.so`，放置在 APK 的 native library 目录
- 直接从 `applicationInfo.nativeLibraryDir` 执行（SELinux 上下文为 `apk_data_file`，可执行）
- 参考 [Termux 实现](https://github.com/termux/termux-app/issues/1072)

#### 符号链接支持

使用 Android `Os` API 正确处理 Docker 镜像中的符号链接：
- `Os.lstat()` + `OsConstants.S_ISLNK()` 检测符号链接
- `Os.readlink()` 读取链接目标
- `Os.symlink()` 创建符号链接
- `Os.chmod()` 设置原始权限

这确保了 Alpine Linux 等依赖符号链接的镜像能正常工作。

### 限制说明

- 无 root 权限限制：部分系统调用可能不可用
- 网络隔离：不支持 Docker 网络功能
- 存储限制：受 Android 应用沙箱限制
- 架构限制：仅支持设备原生架构的容器

### 已验证镜像

- ✅ **Alpine Linux** (latest) - 使用 musl libc 和 BusyBox，完全兼容
- 其他镜像测试中...

### 致谢

- [udocker](https://github.com/indigo-dc/udocker) - 原始概念和参考实现
- [PRoot](https://proot-me.github.io/) - 用户空间 chroot 实现
- [Termux](https://termux.dev/) - Android 终端和 PRoot patches
- [Shizuku](https://shizuku.rikka.app/) - 系统服务访问框架

### 许可证

MIT License

---

## <a id="english"></a>English

### ✨ Key Features

- **Full Internationalization** - Automatic Chinese/English interface switching
- **Image Management** - Pull, delete, view Docker images with QR code support
- **Container Management** - Create, run, stop containers with environment configuration
- **Registry Mirrors** - Built-in China mirrors, Bearer token auth, QR code import
- **Interactive Terminal** - Full container terminal access
- **Rootless** - PRoot-based technology, no root required
- **Shizuku Integration** - Optional Shizuku support to disable Phantom Process Killer

### Project Architecture

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

### Tech Stack

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

### Quick Start

#### Build the Project

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

### Usage Guide

#### 1. Configure Registry Mirror (Optional)

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

#### 2. Pull Images

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

#### 3. Run Containers

- Select image in **Images** → tap **Run** button
- Configure container parameters (name, env vars, working dir, etc.)
- Tap **Create & Run** to start

#### 4. Use Terminal

- Tap **Terminal** button on running container in **Containers** page
- Enter commands in terminal interface

#### 5. Phantom Process Killer (Android 12+)

On Android 12+ devices, the system may kill background processes. Use Shizuku to disable:
1. Install [Shizuku](https://shizuku.rikka.app/)
2. Start Shizuku service
3. Navigate to ADocker **Settings → Phantom Process** to grant permission and disable

### Architecture Highlights

#### Container Status Management

- **Database (ContainerEntity)**: Only stores static configuration, no runtime status
- **Runtime (RunningContainer)**: Tracks active containers in memory
- **UI Layer (ContainerStatus)**: Maps runtime state to UI (CREATED, RUNNING, EXITED)

This design prevents stale status in database (e.g., "RUNNING" status when app is killed).

#### SELinux Compatibility

Android 10+ prevents executing binaries from `app_data_file` directories. Solution:
- PRoot compiled as `libproot.so`, placed in APK's native library directory
- Execute directly from `applicationInfo.nativeLibraryDir` (SELinux context: `apk_data_file`, executable)
- Reference: [Termux implementation](https://github.com/termux/termux-app/issues/1072)

#### Symlink Support

Uses Android `Os` API to properly handle symlinks in Docker images:
- `Os.lstat()` + `OsConstants.S_ISLNK()` detect symlinks
- `Os.readlink()` read link target
- `Os.symlink()` create symlink
- `Os.chmod()` preserve permissions

Ensures Alpine Linux and other symlink-heavy images work correctly.

### Limitations

- Rootless restrictions: Some system calls unavailable
- Network isolation: Docker networking features not supported
- Storage limits: Constrained by Android app sandbox
- Architecture limits: Only native device architecture supported

### Verified Images

- ✅ **Alpine Linux** (latest) - musl libc + BusyBox, fully compatible
- Other images under testing...

### Acknowledgments

- [udocker](https://github.com/indigo-dc/udocker) - Original concept and reference
- [PRoot](https://proot-me.github.io/) - User-space chroot implementation
- [Termux](https://termux.dev/) - Android terminal and PRoot patches
- [Shizuku](https://shizuku.rikka.app/) - System service access framework

### License

MIT License
