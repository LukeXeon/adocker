# Docker Registry Mirrors 镜像源配置

## 可用的镜像源 (测试于 2025-11-23)

经过 `RegistryMirrorConnectivityTest` 测试验证，以下镜像源在中国大陆可用：

### ✅ 可用镜像源

| 镜像源名称 | URL | 状态 | 延迟 | 说明 |
|-----------|-----|------|------|------|
| **DaoCloud (China)** | https://docker.m.daocloud.io | ✅ 可用 | 低 | **默认镜像源** - 推荐使用 |
| **Aliyun (China)** | https://registry.cn-hangzhou.aliyuncs.com | ✅ 可用 | 中等 | 阿里云镜像源 |
| **Huawei Cloud (China)** | https://mirrors.huaweicloud.com | ✅ 可用 | 中等 | 华为云镜像源 |
| **Docker Hub (Official)** | https://registry-1.docker.io | ❌ 被墙 | - | 官方源，保留用于国际环境 |

### ❌ 已移除的镜像源

以下镜像源在测试中不可用，已从内置列表中移除：

| 镜像源名称 | URL | 失败原因 |
|-----------|-----|----------|
| USTC (China) | https://docker.mirrors.ustc.edu.cn | 连接超时/失败 |
| Tencent Cloud (China) | https://mirror.ccs.tencentyun.com | 连接超时/失败 |

## 认证机制

### 中国镜像源

中国镜像源使用以下认证策略（按优先级）：

1. **首选**: 使用镜像源自己的 `/v2/token` 端点进行认证
2. **备选**: 如果认证失败，尝试匿名访问（多数镜像源支持）
3. **禁止**: 不使用 `auth.docker.io`（在中国被墙）

### Docker Hub 官方源

官方源使用标准的 Docker Hub 认证：
- 认证服务器: `https://auth.docker.io`
- 服务名称: `registry.docker.io`

## 使用说明

### 1. 默认配置

应用启动时自动使用 **DaoCloud** 作为默认镜像源，无需配置。

### 2. 切换镜像源

在应用的 **Settings** -> **Registry Mirror** 中可以：
- 查看所有可用镜像源
- 切换到其他镜像源
- 添加自定义镜像源

### 3. 添加自定义镜像源

点击 "Add Custom Mirror" 可以添加自己的镜像源：
```
名称: 我的私有镜像源
URL: https://my-registry.example.com
```

## 测试验证

### 运行连通性测试

```bash
./gradlew :app:connectedDebugAndroidTest --tests "com.adocker.runner.RegistryMirrorConnectivityTest"
```

### 测试内容

1. **testAllMirrorsConnectivity** - 测试所有内置镜像源的连通性
2. **testCurrentMirrorConnectivity** - 测试当前配置的镜像源
3. **testAuthenticationToMirrors** - 测试镜像源的认证功能

### 测试结果示例

```
========================================
Testing connectivity to all registry mirrors
========================================
✓ ACCESSIBLE - DaoCloud (China)
  URL: https://docker.m.daocloud.io
  Latency: 156ms
----------------------------------------
✓ ACCESSIBLE - Aliyun (China)
  URL: https://registry.cn-hangzhou.aliyuncs.com
  Latency: 243ms
----------------------------------------
✓ ACCESSIBLE - Huawei Cloud (China)
  URL: https://mirrors.huaweicloud.com
  Latency: 287ms
----------------------------------------
✗ FAILED - Docker Hub (Official)
  URL: https://registry-1.docker.io
  Error: Connection timeout
========================================
SUMMARY
========================================
Total mirrors tested: 4
Accessible: 3
Failed: 1

*** RECOMMENDED MIRROR ***
Name: DaoCloud (China)
URL: https://docker.m.daocloud.io
Latency: 156ms
========================================
```

## 拉取镜像示例

### 使用默认镜像源（DaoCloud）

```kotlin
// 应用会自动使用 DaoCloud 镜像源
imageRepository.pullImage("alpine:latest")
```

实际请求:
```
GET https://docker.m.daocloud.io/v2/library/alpine/manifests/latest
```

### 镜像名称格式

支持以下格式：
- `alpine:latest` - 简化格式
- `library/alpine:latest` - 完整格式
- `ubuntu:22.04` - 带版本号
- `nginx:1.25-alpine` - 复杂标签

## 故障排除

### 问题 1: 镜像拉取失败

**解决方案**:
1. 检查网络连接
2. 在 Settings 中切换到其他镜像源
3. 查看日志: `adb logcat | grep -i "DockerRegistryApi"`

### 问题 2: 认证失败

**解决方案**:
- 对于中国镜像源：应用会自动尝试匿名访问
- 对于官方 Docker Hub：需要网络能访问 `auth.docker.io`

### 问题 3: 连接超时

**解决方案**:
1. 增加超时时间 (在 `Config.kt` 中配置)
2. 切换到延迟更低的镜像源
3. 检查防火墙/代理设置

## 技术实现

### 镜像源配置文件

- 位置: `app/src/main/java/com/adocker/runner/core/config/RegistrySettings.kt`
- 存储: DataStore Preferences
- 缓存: 内存缓存已选镜像源

### API 客户端

- 位置: `app/src/main/java/com/adocker/runner/data/remote/api/DockerRegistryApi.kt`
- 协议: Docker Registry HTTP API V2
- 客户端: Ktor Client with OkHttp Engine

### 镜像仓库

- 位置: `app/src/main/java/com/adocker/runner/data/repository/ImageRepository.kt`
- 功能: 镜像拉取、层管理、本地存储

## 参考资料

- [Docker Registry HTTP API V2](https://docs.docker.com/registry/spec/api/)
- [Docker Hub 镜像加速器](https://www.docker.org.cn/dockerppt/110.html)
- [国内 Docker 镜像源列表](https://github.com/docker-practice/docker-registry-cn-mirror-test)

## 更新日志

### 2025-11-23
- ✅ 移除 USTC 和 Tencent Cloud 镜像源（连通性测试失败）
- ✅ 优化认证逻辑，中国镜像源避免访问 auth.docker.io
- ✅ 添加 RegistryMirrorConnectivityTest 自动化测试
- ✅ 验证 DaoCloud、Aliyun、Huawei Cloud 三个镜像源可用
