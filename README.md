# ADocker - Android Docker Container Runner

ADocker 是一个在 Android 上运行 Docker 容器的应用，基于 udocker 概念用 Kotlin 重新实现，使用 PRoot 作为执行引擎。

## 功能特性

### 已实现的功能

- **镜像管理**
  - 从 Docker Hub 搜索镜像
  - 拉取镜像 (pull)
  - **扫描二维码添加镜像** ✨ 新功能
  - 查看本地镜像列表
  - 删除本地镜像
  - 导入/导出镜像

- **容器管理**
  - 创建容器
  - 启动/停止容器
  - 删除容器
  - 查看容器列表和状态
  - 容器配置（环境变量、工作目录、命令等）

- **执行引擎**
  - PRoot P1 模式（带 SECCOMP，性能更好）
  - PRoot P2 模式（兼容性更好）
  - Root 模拟（无需实际 root 权限）

- **Registry Mirror（镜像加速）**
  - 内置多个中国镜像源支持
  - 默认使用 DaoCloud 镜像（中国大陆用户优化）
  - 内置镜像源：
    - Docker Hub (Official)
    - DaoCloud (China) - 默认
    - Aliyun (China)
    - USTC (China)
    - Tencent Cloud (China)
    - Huawei Cloud (China)
  - 支持自定义镜像源管理：
    - 添加自定义镜像源
    - 删除自定义镜像源（内置镜像源不可删除）
    - 在独立的镜像源管理页面操作
  - 在设置页面可随时切换和管理镜像源

- **用户界面**
  - Material Design 3 风格
  - 主页仪表盘
  - 镜像管理页面
  - 容器管理页面
  - 终端交互界面
  - 设置页面（含镜像源配置）
  - SplashScreen 启动画面
  - Docker 风格应用图标

- **依赖注入**
  - 使用 Hilt 进行依赖注入
  - 清晰的模块化架构

## 项目结构

```
com.adocker.runner/
├── core/
│   ├── config/        # 配置管理 (Config, RegistrySettings)
│   ├── di/            # 依赖注入 (Hilt AppModule)
│   └── utils/         # 工具类
├── data/
│   ├── local/         # 本地数据库 (Room)
│   │   ├── dao/       # 数据访问对象
│   │   └── entity/    # 数据实体
│   ├── remote/        # 网络API
│   │   ├── api/       # Registry API 客户端
│   │   └── dto/       # 数据传输对象
│   └── repository/    # 数据仓库
├── domain/
│   └── model/         # 领域模型
├── engine/
│   ├── proot/         # PRoot 执行引擎
│   └── executor/      # 容器执行器
└── ui/
    ├── components/    # 通用组件
    ├── navigation/    # 导航配置
    ├── screens/       # 页面
    │   ├── home/
    │   ├── images/
    │   ├── containers/
    │   ├── terminal/
    │   └── settings/
    ├── theme/         # 主题配置
    └── viewmodel/     # ViewModel
```

## 技术栈

- **语言**: Kotlin
- **UI 框架**: Jetpack Compose
- **设计系统**: Material Design 3
- **依赖注入**: Hilt
- **网络**: Ktor Client
- **序列化**: Kotlinx Serialization
- **数据库**: Room
- **异步处理**: Kotlin Coroutines & Flow
- **数据存储**: DataStore Preferences
- **压缩处理**: Apache Commons Compress
- **执行引擎**: PRoot (Termux patched)
- **启动画面**: AndroidX SplashScreen
- **相机处理**: CameraX
- **二维码扫描**: Google ML Kit Barcode Scanning

## PRoot 构建

项目集成了 PRoot 源码构建，使用 CMake 和 Gradle 的 `externalNativeBuild` 功能自动编译。

**PRoot 版本**：v0.15_release（来自 [green-green-avk/proot](https://github.com/green-green-avk/proot)）

**特点**：
- **CMake 集成**：PRoot 源码通过 CMake 编译，与 Gradle 构建系统无缝集成
- **16KB 页面对齐**：支持 Android 15+ (targetSdk 35+) 的 16KB 页面大小要求
- **静态链接 libtalloc**：talloc 库在构建时自动下载并静态链接
- **独立 loader**：使用 unbundled loader 架构
- **多架构支持**：arm64-v8a, armeabi-v7a, x86_64, x86

**项目结构**：
```
app/src/main/cpp/
├── CMakeLists.txt    # PRoot 构建配置
└── proot/            # PRoot 源码 (v0.15_release)
    └── src/          # PRoot 核心源码
```

**构建产物**（自动打包到 APK）：
- `libproot.so` - PRoot 主程序
- `libproot_loader.so` - PRoot loader

**依赖**：
- talloc 2.1.14（构建时自动从 samba.org 下载到 build 目录）

## 编译项目

1. 确保安装了 Android Studio 和 JDK 17+

2. 克隆项目并打开：
```bash
git clone <repository-url>
cd adocker
```

3. 使用 Android Studio 打开项目

4. 同步 Gradle 依赖

5. 编译并运行：
```bash
./gradlew assembleDebug
```

## 使用说明

### 配置镜像源（中国大陆用户）

应用默认已配置 DaoCloud 镜像加速，如需更换或添加自定义镜像：

1. 打开应用，进入 "Settings" 页面
2. 点击 "Registry Mirror" 部分的 "Docker Registry Mirror"
3. 进入镜像源管理页面，您可以：
   - 选择内置的镜像源（Docker Hub, DaoCloud, Aliyun, USTC, Tencent, Huawei）
   - 点击 "Add Custom Mirror" 添加自定义镜像源
   - 删除自定义镜像源（内置镜像源不可删除）
4. 选择镜像源后会立即生效并显示提示信息

**注意**：内置镜像源标记为 "Default"（默认）或不可删除，自定义镜像源标记为 "Custom" 且可以删除。

### 拉取镜像

#### 方式一：手动输入
1. 打开应用，进入 "Images" 页面
2. 点击右上角的下载图标或 FAB
3. 在 "Pull Image" 页面输入镜像名称（如 `alpine:latest`）
4. 点击 "Pull" 按钮等待下载完成

#### 方式二：扫描二维码（推荐）✨

**使用方法**：
1. 进入 "Pull Image" 页面
2. 点击右上角的二维码扫描图标
3. 授予相机权限（首次使用）
4. 将二维码对准扫描框
5. 自动识别后确认并开始拉取

**功能特性**：
- 自动对焦：相机会自动对焦二维码
- 闪光灯控制：适用于暗光环境
- 实时识别：对准即可识别，无需拍照
- 扫描框引导：清晰的视觉引导

**支持的二维码格式**：

1. **简单格式（推荐）**：
   ```
   alpine:latest
   ubuntu:22.04
   nginx:1.25
   ```

2. **JSON 格式**：
   ```json
   {
     "type": "docker-image",
     "image": "ubuntu:22.04",
     "description": "Ubuntu 22.04 LTS"
   }
   ```

3. **URL 格式**：
   ```
   adocker://pull?image=alpine:latest
   ```

**生成测试二维码**：

使用在线工具（如 qr-code-generator.com）或命令行工具：

```bash
# 使用 qrencode (macOS/Linux)
brew install qrencode  # macOS
qrencode -o alpine.png "alpine:latest"

# 使用 Python
pip install qrcode
python -c "import qrcode; qrcode.make('alpine:latest').save('alpine.png')"
```

**技术实现**：
- Google ML Kit Barcode Scanning (v17.3.0)
- CameraX (v1.4.1)
- Jetpack Compose UI

### 创建容器

1. 在 "Images" 页面，点击某个镜像的 "Run" 按钮
2. 配置容器参数（名称、命令、环境变量等）
3. 点击 "Create" 或 "Create & Run"

### 使用终端

1. 在 "Containers" 页面，点击运行中容器的 "Terminal" 按钮
2. 在终端界面输入命令并执行
3. 使用快捷命令芯片快速执行常用命令

## 限制说明

- 无 root 权限限制：部分系统调用可能不可用
- 网络隔离：不支持 Docker 网络功能
- 存储限制：受 Android 应用沙箱限制
- 架构限制：仅支持设备原生架构的容器

## 测试

项目包含完整的单元测试和 UI 测试：

### 运行测试

```bash
# 运行所有测试
./gradlew connectedDebugAndroidTest

# 查看测试报告
open app/build/reports/androidTests/connected/debug/index.html
```

### 测试覆盖

- **基础功能测试** (`BasicFunctionalityTest.kt`)：
  - Config 初始化测试
  - 镜像源配置测试
  - 自定义镜像源添加/删除测试
  - 镜像源切换测试

- **UI 测试** (`ui/` 目录)：
  - `HomeScreenTest.kt` - 主页显示测试
  - `SettingsScreenTest.kt` - 设置页面测试
  - `MirrorSettingsScreenTest.kt` - 镜像源设置页面测试
  - `ImagesScreenTest.kt` - 镜像列表页面测试
  - `ContainersScreenTest.kt` - 容器列表页面测试

- **集成测试** (`ImagePullAndRunTest.kt`)：
  - PRoot 引擎可用性测试 ✅
  - 镜像拉取和容器运行测试（网络不可用时自动跳过）
  - Alpine Linux 容器执行测试 ✅（验证 musl libc 兼容性）

### SELinux 和 PRoot 执行

在 Android 10+ (API 29+) 上，应用无法从 `app_data_file` 目录执行二进制文件（SELinux `execute_no_trans` 限制）。

**问题分析**：
```
avc: denied { execute_no_trans } for path="/data/data/com.adocker.runner/files/bin/proot"
scontext=u:r:untrusted_app:s0 tcontext=u:object_r:app_data_file:s0
```

**解决方案**：参考 [Termux](https://github.com/termux/termux-app/issues/1072) 的实现方式，PRoot 必须直接从 native library 目录执行：

| 目录 | SELinux 上下文 | 可执行 |
|------|---------------|--------|
| `/data/app/<pkg>/lib/<arch>` | `apk_data_file` | ✅ 是 |
| `/data/data/<pkg>` | `app_data_file` | ❌ 否 |

**项目实现**：
1. PRoot 通过 CMake 编译为 `libproot.so`，自动打包到 APK 的 native library 目录
2. PRoot loader 编译为 `libproot_loader.so`，同样打包到 native library 目录
3. `PRootEngine` 直接从 `applicationInfo.nativeLibraryDir` 执行
4. **不复制**二进制文件到 app data 目录

**关键代码** (`AppModule.kt`):
```kotlin
val nativeLibDir = context.applicationInfo.nativeLibraryDir
val prootBinary = File(nativeLibDir, "libproot.so")
// 直接执行，不复制
PRootEngine(prootBinary, nativeLibDir)
```

详细日志可通过 logcat 查看：
```bash
adb logcat -d | grep -i "PRootEngine\|AppModule"
```

### 符号链接处理和 Android Os API

为了正确支持 Docker 镜像中的符号链接（特别是 Alpine Linux 等使用大量符号链接的发行版），项目优先使用 Android 的原生 `Os` 类 API，而不是 JDK 的文件 API。

**技术背景**：
- Alpine Linux 使用 BusyBox，其中 `/bin/sh` 等工具都是指向 `/bin/busybox` 的符号链接
- Docker 镜像层使用 tar 格式，包含符号链接和硬链接
- Java 的 `File` API 无法正确处理符号链接的创建和检测

**实现方案**：

1. **符号链接处理** (`FileUtils.kt`, `ContainerRepository.kt`)：
   - 使用 `Os.lstat()` 检测符号链接（而不是 `Files.isSymbolicLink()`）
   - 使用 `OsConstants.S_ISLNK()` 判断文件类型
   - 使用 `Os.readlink()` 读取符号链接目标
   - 使用 `Os.symlink()` 创建符号链接（而不是 `Files.createSymbolicLink()`）

2. **硬链接处理** (`FileUtils.kt`)：
   - 使用 `Os.link()` 创建硬链接

3. **权限设置** (`FileUtils.kt`, `ContainerRepository.kt`)：
   - 使用 `Os.chmod()` 设置文件权限（而不是 `File.setReadable/setWritable/setExecutable()`）
   - 直接使用 tar 文件中的原始权限模式

**关键代码示例** (`ContainerRepository.kt:151-168`)：
```kotlin
// 检测符号链接
val stat = Os.lstat(file.absolutePath)
if (OsConstants.S_ISLNK(stat.st_mode)) {
    // 读取链接目标
    val linkTarget = Os.readlink(file.absolutePath)
    // 在目标位置重新创建符号链接
    Os.symlink(linkTarget, destFile.absolutePath)
}
```

**验证**：
- Alpine Linux 容器测试通过，所有符号链接正确创建
- musl libc 正常工作（`/lib/ld-musl-aarch64.so.1` 可执行）
- BusyBox 工具链完全可用（`sh`, `ls`, `cat` 等）

## 限制说明

- 无 root 权限限制：部分系统调用可能不可用
- 网络隔离：不支持 Docker 网络功能
- 存储限制：受 Android 应用沙箱限制
- 架构限制：仅支持设备原生架构的容器

## 已验证的镜像

以下镜像已通过测试，可以在 ADocker 上正常运行：
- ✅ **Alpine Linux** (latest) - 使用 musl libc 和 BusyBox
- 其他镜像正在测试中...

## 许可证

MIT License

## 致谢

- [udocker](https://github.com/indigo-dc/udocker) - 原始概念
- [PRoot](https://proot-me.github.io/) - 用户空间 chroot 实现
- [Termux](https://termux.dev/) - Android 终端模拟器和 PRoot patches
- Docker Hub 中国镜像源提供商（DaoCloud, Aliyun, USTC, Tencent, Huawei）


## TODO
- 将proot构建集成到项目中