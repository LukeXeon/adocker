# ADocker - Android Docker Container Runner

ADocker 是一个在 Android 上运行 Docker 容器的应用，基于 udocker 概念用 Kotlin 重新实现，使用 PRoot 作为执行引擎。

## ✨ 核心特性

- **完整国际化支持** - 中英文界面自动切换
- **镜像管理** - 拉取、删除、查看 Docker 镜像，支持二维码扫描
- **容器管理** - 创建、运行、停止容器，支持环境变量配置
- **镜像加速** - 内置中国镜像源，支持 Bearer Token 认证，二维码导入配置
- **交互终端** - 完整的容器终端访问
- **无需 Root** - 基于 PRoot 技术，无需 root 权限
- **Shizuku 集成** - 可选使用 Shizuku 禁用 Phantom Process Killer，提升稳定性

## 项目结构

```
com.adocker.runner/
├── core/
│   ├── config/        # 配置管理 (AppConfig)
│   ├── di/            # 依赖注入 (Hilt AppModule)
│   ├── logging/       # 日志配置 (Timber初始化)
│   ├── startup/       # 应用启动初始化
│   └── utils/         # 工具类 (FileUtils, PhantomProcessManager)
├── data/
│   ├── local/         # 本地数据库 (Room)
│   │   ├── dao/       # 数据访问对象
│   │   └── model/     # 数据实体 (Entity类)
│   ├── remote/        # 网络API
│   │   ├── api/       # Docker Registry API 客户端
│   │   └── model/     # 远程数据模型 (Response类)
│   └── repository/    # 数据仓库层
│       └── model/     # 仓库模型 (业务模型)
├── engine/
│   ├── proot/         # PRoot 执行引擎
│   └── executor/      # 容器执行器
└── ui/
    ├── components/    # 通用UI组件
    ├── navigation/    # 导航配置
    ├── screens/       # 页面
    │   ├── home/      # 主页
    │   ├── images/    # 镜像管理
    │   ├── containers/# 容器管理
    │   ├── terminal/  # 终端界面
    │   ├── settings/  # 设置页面
    │   └── qrcode/    # 二维码扫描
    ├── theme/         # Material3 主题
    └── viewmodel/     # ViewModel层
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

**环境要求**: Android Studio + JDK 17+

## 使用说明

### 1. 配置镜像加速（可选）

进入 **Settings → Docker Registry Mirror**:
- 选择内置镜像源（Docker Hub, DaoCloud, Aliyun, Huawei）
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

### 2. 拉取镜像

**方式一：手动输入**
- 进入 **Images** 页面 → 点击 **+** 按钮
- 输入镜像名称（如 `alpine:latest`）
- 点击 **Pull** 开始下载

**方式二：扫描二维码（推荐）**
- 进入 **Pull Image** 页面 → 点击二维码图标
- 扫描包含镜像名称的二维码（如 `alpine:latest`）
- 自动填充并开始拉取

### 3. 运行容器

- 在 **Images** 页面选择镜像 → 点击 **Run** 按钮
- 配置容器参数（名称、环境变量、工作目录等）
- 点击 **Create & Run** 启动容器

### 4. 使用终端

- 在 **Containers** 页面点击运行中容器的 **Terminal** 按钮
- 在终端界面输入命令并执行

### 5. Phantom Process Killer（Android 12+）

在 Android 12+ 设备上，系统可能会杀死后台进程。使用 Shizuku 可以禁用此限制：
1. 安装 [Shizuku](https://shizuku.rikka.app/)
2. 启动 Shizuku 服务
3. 进入 ADocker **Settings → Phantom Process** 授权并禁用 Killer

## 限制说明

- 无 root 权限限制：部分系统调用可能不可用
- 网络隔离：不支持 Docker 网络功能
- 存储限制：受 Android 应用沙箱限制
- 架构限制：仅支持设备原生架构的容器

## 已验证镜像

- ✅ **Alpine Linux** (latest) - 使用 musl libc 和 BusyBox
- 其他镜像测试中...

## 技术细节

### SELinux 限制与解决方案

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

## 致谢

- [udocker](https://github.com/indigo-dc/udocker) - 原始概念和参考实现
- [PRoot](https://proot-me.github.io/) - 用户空间 chroot 实现
- [Termux](https://termux.dev/) - Android 终端和 PRoot patches
- [Shizuku](https://shizuku.rikka.app/) - 系统服务访问框架

## 许可证

MIT License