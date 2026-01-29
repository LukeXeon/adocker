# Andock - Android Docker 容器运行器

**[English](README.en.md) | 中文**

Andock 是一个在 Android 上运行 Docker 容器的应用，基于 udocker 概念用 Kotlin 重新实现，使用 PRoot 作为执行引擎，无需 root 权限。

## ✨ 核心特性

- **🐳 完整容器管理** - 创建、启动、停止、删除容器，支持环境变量和工作目录配置
- **📦 镜像管理** - 从 Docker Registry 拉取镜像，查看镜像详情，删除不需要的镜像
- **🔍 Docker Hub 搜索** - 集成 Docker Hub 搜索，支持无限滚动分页（Paging 3）
- **🌍 镜像加速** - 内置多个中国镜像源，支持自定义镜像源和 Bearer Token 认证
- **📱 二维码导入** - 使用 CameraX + ML Kit 扫描二维码快速导入镜像源配置
- **💻 交互终端** - 完整的容器终端访问，支持 exec 执行命令
- **🚀 无需 Root** - 基于 PRoot v0.15 技术，完全运行在用户空间
- **🔧 Shizuku 集成** - 可选使用 Shizuku 禁用 Android 12+ 的 Phantom Process Killer
- **🌐 Docker API 服务器** - 内置 Docker Engine API v1.45 兼容的 HTTP 服务器（Unix Socket）
- **🎨 Material Design 3** - 遵循 Google 最新设计规范，深色/浅色主题自动切换
- **🌏 完整国际化** - 中英文界面自动切换

## 🏗️ 项目架构

项目采用多模块架构，清晰分离业务逻辑和 UI 层：

```
andock/
├── daemon/                           # 核心业务逻辑模块 (Android Library)
│   ├── app/                          # 应用配置和初始化
│   │   ├── AppContext.kt             # 应用上下文和目录配置
│   │   ├── AppInitializer.kt         # App Startup 初始化
│   │   └── AppModule.kt              # Hilt 依赖注入模块
│   │
│   ├── containers/                   # 容器管理
│   │   ├── Container.kt              # 容器实例（FlowRedux 状态机）
│   │   ├── ContainerManager.kt       # 容器创建、追踪、删除
│   │   ├── ContainerState.kt         # 8 种状态（Created, Starting, Running...）
│   │   └── ContainerStateMachine.kt  # 状态机逻辑
│   │
│   ├── database/                     # Room 数据库
│   │   ├── AppDatabase.kt            # 数据库定义（版本 1）
│   │   ├── dao/                      # 数据访问对象
│   │   │   ├── ImageDao.kt
│   │   │   ├── LayerDao.kt
│   │   │   ├── ContainerDao.kt
│   │   │   ├── RegistryDao.kt
│   │   │   ├── AuthTokenDao.kt
│   │   │   └── SearchRecordDao.kt
│   │   └── model/                    # 数据库实体
│   │       ├── ImageEntity.kt
│   │       ├── LayerEntity.kt
│   │       ├── LayerReferenceEntity.kt
│   │       ├── ContainerEntity.kt
│   │       ├── RegistryEntity.kt
│   │       ├── AuthTokenEntity.kt
│   │       └── SearchRecordEntity.kt
│   │
│   ├── engine/                       # PRoot 执行引擎
│   │   ├── PRootEngine.kt            # PRoot 命令构建和执行
│   │   ├── PRootEnvironment.kt       # 环境变量配置
│   │   └── PRootVersion.kt           # 版本检测
│   │
│   ├── images/                       # 镜像管理
│   │   ├── ImageManager.kt           # 镜像仓库操作
│   │   ├── ImageClient.kt            # Docker Registry API 客户端
│   │   ├── ImageReference.kt         # 镜像名称解析器
│   │   ├── downloader/               # 镜像下载器
│   │   │   ├── ImageDownloader.kt    # 下载状态机
│   │   │   └── ImageDownloadState.kt # 下载状态
│   │   └── model/                    # Registry API 模型
│   │
│   ├── http/                         # HTTP 服务器
│   │   ├── UnixHttp4kServer.kt       # Unix Socket HTTP 服务器
│   │   ├── TcpHttp4kServer.kt        # TCP HTTP 服务器（调试用）
│   │   └── HttpProcessor.kt          # HTTP/1.1 协议处理器
│   │
│   ├── server/                       # Docker API 服务器
│   │   ├── DockerApiServer.kt        # 主服务器（组合所有路由）
│   │   └── routes/                   # Docker API v1.45 路由模块
│   │       ├── ContainerRoutes.kt
│   │       ├── ImageRoutes.kt
│   │       ├── SystemRoutes.kt
│   │       ├── VolumeRoutes.kt
│   │       ├── ExecRoutes.kt
│   │       └── ... (更多路由)
│   │
│   ├── registries/                   # 镜像源管理
│   │   ├── Registry.kt               # 镜像源实例（FlowRedux 状态机）
│   │   ├── RegistryManager.kt        # 镜像源追踪和健康检查
│   │   └── RegistryStateMachine.kt   # 健康检查状态机
│   │
│   ├── search/                       # Docker Hub 搜索
│   │   ├── SearchPagingSource.kt     # Paging 3 数据源
│   │   ├── SearchRepository.kt       # 搜索仓库
│   │   ├── SearchHistory.kt          # DataStore 搜索历史
│   │   └── model/                    # 搜索结果模型
│   │
│   ├── io/                           # 文件 I/O 工具
│   │   ├── File.kt                   # 符号链接处理、SHA256 计算
│   │   ├── Zip.kt                    # Tar/GZ 解压（保留符号链接）
│   │   └── Tailer.kt                 # 日志尾随读取
│   │
│   ├── os/                           # 操作系统集成
│   │   ├── ProcessLimitCompat.kt     # Phantom Process Killer 管理
│   │   ├── RemoteProcess.kt          # Shizuku 进程包装器
│   │   └── Process.kt                # 进程执行工具
│   │
│   └── logging/                      # 日志系统
│       ├── TimberLogger.kt           # Timber + SLF4J 集成
│       └── TimberServiceProvider.kt  # SLF4J 服务提供者
│
├── proot/                            # PRoot 原生编译模块 (Android Library)
│   └── src/main/cpp/
│       ├── CMakeLists.txt            # CMake 构建配置
│       └── scripts/                  # 编译脚本
│           ├── build-talloc.sh       # 编译 talloc 依赖库
│           ├── build-proot.sh        # 编译 PRoot
│           └── filter-output.sh      # 输出过滤器
│
└── app/                              # UI 模块 (Android Application)
    ├── AndockApplication.kt          # Application 类
    ├── ui/
    │   ├── MainActivity.kt           # 单 Activity + Compose
    │   │
    │   ├── screens/                  # 页面组件（按功能分组）
    │   │   ├── main/                 # 主导航页面
    │   │   │   └── MainScreen.kt     # 底部导航栏
    │   │   ├── home/                 # 主页（仪表盘）
    │   │   │   ├── HomeScreen.kt
    │   │   │   └── HomeViewModel.kt
    │   │   ├── containers/           # 容器管理
    │   │   │   ├── ContainersScreen.kt       # 容器列表
    │   │   │   ├── ContainerDetailScreen.kt  # 容器详情
    │   │   │   ├── ContainerCreateScreen.kt  # 创建容器
    │   │   │   └── ContainersViewModel.kt
    │   │   ├── images/               # 镜像管理
    │   │   │   ├── ImagesScreen.kt
    │   │   │   ├── ImageDetailScreen.kt
    │   │   │   ├── ImagePullDialog.kt
    │   │   │   └── ImagesViewModel.kt
    │   │   ├── search/               # Docker Hub 搜索
    │   │   │   ├── SearchScreen.kt
    │   │   │   ├── SearchFilterPanel.kt
    │   │   │   ├── SearchHistoryPanel.kt
    │   │   │   └── SearchViewModel.kt
    │   │   ├── registries/           # 镜像源管理
    │   │   │   ├── RegistriesScreen.kt
    │   │   │   ├── AddMirrorScreen.kt
    │   │   │   └── RegistriesViewModel.kt
    │   │   ├── qrcode/               # 二维码扫描
    │   │   │   ├── QrcodeScannerScreen.kt
    │   │   │   └── QrcodeCamera.kt
    │   │   ├── terminal/             # 容器终端
    │   │   │   ├── TerminalScreen.kt
    │   │   │   └── TerminalViewModel.kt
    │   │   ├── limits/               # 进程限制管理
    │   │   │   └── ProcessLimitScreen.kt
    │   │   └── settings/             # 设置页面
    │   │       ├── SettingsScreen.kt
    │   │       └── SettingsViewModel.kt
    │   │
    │   ├── components/               # 可复用 UI 组件
    │   │   ├── DetailCard.kt
    │   │   ├── StatusIndicator.kt
    │   │   ├── LoadingDialog.kt
    │   │   └── ...
    │   │
    │   └── theme/                    # Material Design 3 主题
    │       ├── Spacing.kt            # 间距常量（8dp 网格）
    │       └── IconSize.kt           # 图标尺寸常量
    │
    └── build/                        # 构建输出（自动生成）
        └── intermediates/jniLibs/    # PRoot 二进制文件（由 proot 模块编译）
            ├── arm64-v8a/
            │   ├── libproot.so           # PRoot 可执行文件
            │   ├── libproot_loader.so    # 64 位加载器
            │   └── libproot_loader32.so  # 32 位加载器
            ├── armeabi-v7a/
            ├── x86_64/
            └── x86/
```

## 🔧 技术栈

### 核心框架
- **Kotlin 2.2.21** - 100% Kotlin 代码
- **Jetpack Compose** - 现代化声明式 UI (BOM 2025.12.00)
- **Material Design 3** - Google 最新设计规范
- **Coroutines & Flow 1.10.2** - 异步响应式编程

### 依赖注入
- **Hilt 2.57.2** - 依赖注入框架
- **AssistedInject** - 动态实例创建（Container, ImageDownloader, Registry）

### 数据层
- **Room 2.8.4** - 本地 SQLite 数据库
- **DataStore** - 轻量级键值存储（搜索历史）
- **Paging 3.3.5** - 分页加载（Docker Hub 搜索）

### 网络通信
- **Ktor 3.3.3** - HTTP 客户端（OkHttp 引擎）
- **kotlinx-serialization 1.9.0** - JSON 序列化
- **http4k 6.23.1.0** - HTTP 服务器框架

### 状态管理
- **FlowRedux 2.0.0** - 状态机框架（容器、下载、镜像源）

### 文件处理
- **Apache Commons Compress 1.28.0** - Tar/GZ 解压
- **XZ 1.11** - .tar.xz 格式支持

### 图像加载
- **Coil 3.3.0** - 图片加载库（Compose 集成）

### 相机与扫码
- **CameraX 1.5.2** - 相机 API
- **ML Kit Barcode Scanning 17.3.0** - 二维码识别

### 系统集成
- **Shizuku 13.1.5** - 系统权限管理
- **PRoot v0.15** - 用户空间 chroot（从源码自动编译，基于 [LukeXeon/proot](https://github.com/LukeXeon/proot)）
- **talloc 2.4.2** - PRoot 内存管理依赖库

### 日志
- **Timber 5.0.1** - Android 日志库
- **SLF4J 2.0.17** - 统一日志接口

### 导航
- **Navigation Compose 2.9.6** - 类型安全导航

## 🚀 快速开始

### 环境要求

- **Android Studio** Ladybug (2024.2.1) 或更高版本
- **JDK 17+**
- **Android SDK API 36**
- **设备要求**: Android 8.0+ (API 26+)

### 编译项目

```bash
# 1. 克隆项目
git clone <repository-url>
cd andock

# 2. 使用 Android Studio 打开项目并同步 Gradle

# 3. 编译 Debug APK
./gradlew assembleDebug

# 4. 安装到设备（需要连接 ADB）
export PATH="$PATH:$HOME/Library/Android/sdk/platform-tools"
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 运行测试

```bash
# 设置环境变量
export PATH="$PATH:$HOME/Library/Android/sdk/platform-tools"
export JAVA_HOME=/opt/homebrew/opt/openjdk@17  # macOS

# 运行所有测试
./gradlew connectedAndroidTest

# 运行特定测试
./gradlew connectedDebugAndroidTest --tests com.github.andock.ImagePullAndRunTest
```

## 📖 使用指南

### 1. 配置镜像加速（可选但推荐）

**步骤:**
1. 打开应用，点击 **Registries** 标签
2. 选择内置镜像源或添加自定义源

**内置镜像源:**
- **Docker Hub** (官方) - registry-1.docker.io
- **DaoCloud** (中国) - docker.m.daocloud.io
- **Xuanyuan** (中国) - docker.xuanyuan.me
- **Huawei Cloud** (中国) - mirrors.huaweicloud.com

**二维码导入格式:**
```json
{
  "name": "My Custom Mirror",
  "url": "https://mirror.example.com",
  "bearerToken": "optional_token_here"
}
```

### 2. 搜索和拉取镜像

**方式一: Docker Hub 搜索**
1. 点击 **Search** 标签
2. 输入关键词（如 `alpine`, `nginx`, `ubuntu`）
3. 点击搜索结果中的下载按钮

**方式二: 直接拉取**
1. 点击 **Images** 标签 → 点击 **+** 按钮
2. 输入完整镜像名称（如 `alpine:latest`）
3. 点击 **Pull** 开始下载

**方式三: 二维码扫描**
1. 点击 **Images** 标签 → 点击扫码图标
2. 扫描包含镜像配置的二维码

### 3. 运行容器

**步骤:**
1. 在 **Images** 页面选择镜像 → 点击 **Run** 按钮
2. 配置容器参数:
   - **容器名称** (自动生成或自定义)
   - **环境变量** (如 `KEY=VALUE`)
   - **工作目录** (可选)
   - **命令** (覆盖默认 CMD，可选)
3. 点击 **Create** 创建并启动容器

### 4. 管理容器

在 **Containers** 页面可以:
- 使用筛选器查看容器（全部/运行中/已退出）
- 启动或停止容器
- 查看容器详情（环境变量、端口、网络等）
- 打开终端执行命令
- 删除不需要的容器

**容器状态:**
- **Created** - 已创建但未启动
- **Starting** - 正在启动中
- **Running** - 运行中
- **Stopping** - 正在停止
- **Exited** - 已退出
- **Dead** - 进程意外终止
- **Removing** - 正在删除
- **Removed** - 已删除

### 5. 使用终端

**步骤:**
1. 在 **Containers** 页面点击运行中容器的 **Terminal** 按钮
2. 在终端界面输入命令并执行
3. 支持快捷命令按钮（如 `ls`, `pwd`, `ps`）

### 6. 禁用 Phantom Process Killer（Android 12+）

在 Android 12+ 设备上，系统可能会杀死后台进程。使用 Shizuku 可以禁用此限制：

**步骤:**
1. 安装 [Shizuku](https://shizuku.rikka.app/)
2. 启动 Shizuku 服务
3. 打开应用，进入 **Settings** → **Background Process Limit**
4. 授权 Shizuku 权限
5. 点击 **Disable Limit** 按钮

**原理:**
```bash
# Android 12L+ (API 32+)
settings put global settings_enable_monitor_phantom_procs false

# Android 12 (API 31)
device_config set_sync_disabled_for_tests persistent
device_config put activity_manager max_phantom_processes 2147483647
```

## 🏛️ 架构亮点

### 1. 容器状态管理（FlowRedux 状态机）

**设计决策: 数据库不存储运行状态**

- **数据库 (ContainerEntity)**: 仅存储静态配置（名称、镜像、创建时间、配置）
- **运行时 (Container)**: 每个容器实例维护自己的状态机，追踪 8 种状态
- **UI 层**: 直接观察 `Container.state` StateFlow，实时更新

**状态转换:**
```
Created → Starting → Running → Stopping → Exited → Removing → Removed
                        ↓
                     Dead
```

**优势:**
- 避免数据库中状态过期（应用被杀死时"Running"状态失效）
- UI 自动实时更新：状态改变时自动重组
- 类型安全的状态转换：状态机确保转换合法性
- 精确的状态表达：8 种状态完整表达容器生命周期

**示例代码:**
```kotlin
@Composable
fun ContainerCard(container: Container) {
    // 观察状态变化，状态改变时自动重组
    val containerState by container.state.collectAsState()

    // 直接使用状态进行 UI 逻辑判断
    when (containerState) {
        is ContainerState.Running -> ShowRunningUI()
        is ContainerState.Created -> ShowCreatedUI()
        is ContainerState.Exited -> ShowExitedUI()
        else -> ShowDefaultUI()
    }

    // 显示状态名称
    Text(text = containerState::class.simpleName ?: "Unknown")
}
```

### 2. 层存储策略（压缩优先）

**设计决策: 仅存储压缩层，按需解压**

**存储结构:**
```
layersDir/
└── {sha256-digest}.tar.gz    # 压缩层文件（2-5 MB）

containersDir/
└── {containerId}/
    └── rootfs/               # 解压后的容器文件系统（7-15 MB）
```

**优势:**
- **70% 存储节省** (Alpine: 3MB 压缩 vs 10MB 解压)
- 更快的镜像拉取（无需解压）
- 更简单的层管理（无需去重）

**权衡:** 容器创建时需要解压，但每个容器只需解压一次

### 3. SELinux 兼容（Android 10+）

**问题:** Android 10+ 禁止从 `app_data_file` 目录执行二进制文件

**解决方案:**
1. `proot` 模块在构建时自动下载并编译 PRoot v0.15 + talloc 2.4.2
2. 编译输出为 `libproot.so`，打包到 APK 的 `jniLibs/` 目录
3. Android 自动提取到 `nativeLibraryDir`，SELinux 上下文为 `apk_data_file`（可执行）
4. 直接从 `applicationInfo.nativeLibraryDir` 执行

**16KB 页面对齐:** 为 Android 15+ 设备配置了 `-Wl,-z,max-page-size=16384` 链接选项

**参考:** [Termux 实现](https://github.com/termux/termux-app/issues/1072)

### 4. 符号链接支持（Os API）

Docker 镜像（尤其是 Alpine Linux）大量使用符号链接。标准 Java `Files` API 不保留它们。

**解决方案:** 使用 Android `Os` API 处理符号链接
```kotlin
- Os.lstat() + OsConstants.S_ISLNK() // 检测符号链接
- Os.readlink()                      // 读取链接目标
- Os.symlink()                       // 创建符号链接
- Os.chmod()                         // 保留权限
```

确保 Alpine Linux 等依赖符号链接的镜像能正常工作。

### 5. Unix Socket HTTP 服务器（http4k）

**实现:** `UnixHttp4kServer` 支持 FILESYSTEM 和 ABSTRACT 两种命名空间

**命名空间对比:**

| 特性 | FILESYSTEM | ABSTRACT |
|------|-----------|----------|
| **文件创建** | ✅ 创建 socket 文件 | ❌ 无文件（内存） |
| **可见性** | `ls`, `stat` 可见 | 文件系统不可见 |
| **清理** | ⚠️ 需手动删除 | ✅ 关闭时自动清理 |
| **权限** | 受文件权限约束 | N/A |
| **用途** | 调试、CLI 工具 | 应用内部 IPC |

**Docker API 集成:**
```kotlin
UnixHttp4kServer(
    name = File(appConfig.filesDir, "docker.sock").absolutePath,
    namespace = Namespace.FILESYSTEM,
    httpHandler = routes.reduce { acc, handler -> acc.then(handler) }
)
```

### 6. Docker Hub 搜索（Paging 3）

**URL 分页实现:**
- **PagingSource Key:** URL (String?)
- **初始请求:** `/v2/search/repositories/?query={q}&page_size=25`
- **后续请求:** 跟随响应中的 `next` URL

**特性:**
- 防抖搜索（400ms 延迟）
- 搜索历史（DataStore，最多 20 条）
- UI 侧筛选（官方镜像、最低星数）
- 实时拉取状态追踪

## ⚠️ 限制说明

- **无 root 权限限制**: 部分系统调用不可用（如 `mount`, `chroot`）
- **网络隔离**: 不支持 Docker 网络功能（bridge, overlay）
- **存储限制**: 所有数据存储在应用内部存储中
- **架构限制**: 仅支持设备原生架构的容器（不支持模拟）
- **性能**: PRoot 引入约 10-15% 性能开销

## ✅ 已验证镜像

- ✅ **Alpine Linux** (latest) - 使用 musl libc 和 BusyBox，完全兼容
- ✅ **BusyBox** (latest) - 最小化工具集，完全兼容
- ⚠️ **Ubuntu** (需要较大存储空间，部分功能受限)
- ⚠️ **Nginx** (受网络限制影响)
- 其他镜像测试中...

## 📂 数据目录

```
/data/data/com.github.andock/
├── files/
│   ├── containers/
│   │   └── {containerId}/
│   │       └── rootfs/           # 容器文件系统
│   └── layers/
│       └── {digest}.tar.gz       # 压缩层文件
├── cache/
│   ├── log/
│   │   └── {containerId}/
│   │       ├── stdout            # 容器标准输出
│   │       └── stderr            # 容器标准错误
│   └── docker.sock               # Docker API Unix Socket
└── databases/
    └── andock.db                # Room 数据库
```

## 🙏 致谢

- [udocker](https://github.com/indigo-dc/udocker) - 原始概念和参考实现
- [PRoot](https://proot-me.github.io/) - 用户空间 chroot 实现
- [Termux](https://termux.dev/) - Android 终端和 PRoot patches
- [Shizuku](https://shizuku.rikka.app/) - 系统服务访问框架
- [LukeXeon/proot](https://github.com/LukeXeon/proot) - PRoot for Android（基于 green-green-avk/proot 的 fork）

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。
