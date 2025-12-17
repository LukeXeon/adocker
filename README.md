# ADocker - Android Docker Container Runner

**[English](README.en.md) | 中文**

ADocker 是一个在 Android 上运行 Docker 容器的应用，基于 udocker 概念用 Kotlin 重新实现，使用 PRoot 作为执行引擎。

## ✨ 核心特性

- **完整国际化支持** - 中英文界面自动切换
- **镜像管理** - 拉取、删除、查看 Docker 镜像，支持二维码扫描
- **容器管理** - 创建、运行、停止容器，支持环境变量配置
- **镜像加速** - 内置中国镜像源，支持 Bearer Token 认证，二维码导入配置
- **交互终端** - 完整的容器终端访问
- **无需 Root** - 基于 PRoot 技术，无需 root 权限
- **Shizuku 集成** - 可选使用 Shizuku 禁用 Phantom Process Killer，提升稳定性

## 界面预览

应用采用 5 个底部标签页的现代化设计：

| 标签页 | 功能描述 |
|-------|---------|
| **主页** | 欢迎页面、统计概览、快捷操作入口 |
| **发现** | 搜索 Docker Hub 镜像，支持分页浏览 |
| **容器** | 容器列表，按状态筛选（全部/已创建/运行中/已退出） |
| **镜像** | 本地镜像管理，扫码添加，一键运行 |
| **设置** | 镜像源配置、后台进程管理、存储管理 |

## 项目架构

项目采用多模块架构，清晰分离业务逻辑和UI层：

```
adocker/
├── daemon/                  # 核心业务逻辑模块 (Android Library)
│   ├── config/             # 配置管理
│   ├── database/           # Room数据库、DAO、Entity
│   ├── di/                 # 依赖注入
│   ├── containers/         # 容器执行和管理
│   ├── images/             # 镜像仓库
│   ├── registry/           # Docker Registry API
│   ├── os/                 # 系统集成 (PhantomProcessManager)
│   ├── utils/              # 工具类
│   └── startup/            # 应用初始化
└── app/                     # UI模块 (Android Application)
    └── ui/
        ├── model/          # UI层数据模型
        ├── screens/        # 页面
        │   ├── home/       # 主页
        │   ├── discover/   # 发现页
        │   ├── containers/ # 容器页
        │   ├── images/     # 镜像页
        │   ├── settings/   # 设置页
        │   ├── terminal/   # 终端页
        │   └── qrcode/     # 二维码扫描
        ├── viewmodel/      # ViewModel层
        ├── components/     # 通用UI组件
        ├── navigation/     # 导航配置
        └── theme/          # Material3 主题
```

## 技术栈

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

## 快速开始

### 编译项目

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

## 使用说明

### 1. 配置镜像加速（可选）

进入 **设置 → Docker 镜像源**:
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

### 2. 搜索和拉取镜像

**方式一：发现页搜索**
- 进入 **发现** 标签页
- 输入关键词搜索 Docker Hub（如 `alpine`, `nginx`）
- 点击搜索结果中的下载按钮拉取镜像

**方式二：直接拉取**
- 进入 **镜像** 标签页 → 点击 **+** 按钮
- 输入完整镜像名称（如 `alpine:latest`）
- 点击 **拉取** 开始下载

**方式三：扫描二维码**
- 进入 **镜像** 标签页 → 点击扫码图标
- 扫描包含镜像配置的二维码

### 3. 运行容器

- 在 **镜像** 页面选择镜像 → 点击 **运行** 按钮
- 配置容器参数（名称、环境变量、工作目录等）
- 点击 **创建** 启动容器
- 容器创建成功后自动跳转到 **容器** 页面

### 4. 管理容器

在 **容器** 页面可以：
- 使用筛选器按状态查看容器（全部/已创建/运行中/已退出）
- 启动或停止容器
- 打开终端执行命令
- 删除不需要的容器

### 5. 使用终端

- 在 **容器** 页面点击运行中容器的 **终端** 按钮
- 在终端界面输入命令并执行

### 6. Phantom Process Killer（Android 12+）

在 Android 12+ 设备上，系统可能会杀死后台进程。使用 Shizuku 可以禁用此限制：
1. 安装 [Shizuku](https://shizuku.rikka.app/)
2. 启动 Shizuku 服务
3. 进入 **设置 → 后台进程限制** 授权并禁用限制

## 架构亮点

### 容器状态管理

- **数据库（ContainerEntity）**：只存储静态配置，不存储运行状态
- **运行时（Container + ContainerStateMachine）**：每个容器维护自己的状态机，追踪8种状态（Created, Starting, Running, Stopping, Exited, Dead, Removing, Removed）
- **UI层**：直接使用 `ContainerState` 子类名称显示，通过 `StateFlow` 实时观察状态变化

这种设计的优势：
- 避免了数据库中状态过期的问题（如应用被杀死时容器"RUNNING"状态会失效）
- UI 自动实时更新：当容器状态改变时，UI 会自动重组
- 类型安全的状态转换：通过状态机确保状态转换的合法性
- 精确的状态表达：8种状态完整表达容器生命周期，无信息丢失

#### 实时状态观察

在 UI 组件中观察容器状态：
```kotlin
@Composable
fun ContainerCard(container: Container) {
    // 观察状态变化，状态改变时自动重组
    val containerState by container.state.collectAsState()

    // 直接使用状态进行UI逻辑判断
    when (containerState) {
        is ContainerState.Running -> ShowStopButton()
        else -> ShowStartButton()
    }

    // 显示状态名称
    Text(text = containerState::class.simpleName ?: "Unknown")
}
```

### SELinux 兼容

Android 10+ 禁止从 `app_data_file` 目录执行二进制文件。解决方案：
- PRoot 编译为 `libproot.so`，放置在 APK 的 native library 目录
- 直接从 `applicationInfo.nativeLibraryDir` 执行（SELinux 上下文为 `apk_data_file`，可执行）
- 参考 [Termux 实现](https://github.com/termux/termux-app/issues/1072)

### 符号链接支持

使用 Android `Os` API 正确处理 Docker 镜像中的符号链接：
- `Os.lstat()` + `OsConstants.S_ISLNK()` 检测符号链接
- `Os.readlink()` 读取链接目标
- `Os.symlink()` 创建符号链接
- `Os.chmod()` 设置原始权限

这确保了 Alpine Linux 等依赖符号链接的镜像能正常工作。

## 限制说明

- 无 root 权限限制：部分系统调用可能不可用
- 网络隔离：不支持 Docker 网络功能
- 存储限制：所有数据存储在内部存储中
- 架构限制：仅支持设备原生架构的容器

## 已验证镜像

- ✅ **Alpine Linux** (latest) - 使用 musl libc 和 BusyBox，完全兼容
- 其他镜像测试中...

## 致谢

- [udocker](https://github.com/indigo-dc/udocker) - 原始概念和参考实现
- [PRoot](https://proot-me.github.io/) - 用户空间 chroot 实现
- [Termux](https://termux.dev/) - Android 终端和 PRoot patches
- [Shizuku](https://shizuku.rikka.app/) - 系统服务访问框架

## 许可证

MIT License
