# VirGL vtest Server 实现完整教程

## 目录

1. [架构概述](#1-架构概述)
2. [环境准备](#2-环境准备)
3. [VirGL 初始化](#3-virgl-初始化)
4. [vtest 协议详解](#4-vtest-协议详解)
5. [实现 Server 核心](#5-实现-server-核心)
6. [完整示例代码](#6-完整示例代码)
7. [调试与测试](#7-调试与测试)
8. [Android 集成](#8-android-集成)

---

## 1. 架构概述

### 1.1 VirGL 是什么？

**VirGL (Virtual OpenGL)** 是一个 GPU 虚拟化解决方案，允许客户机（如容器、虚拟机）通过主机的 GPU 进行硬件加速渲染。

```
┌─────────────────────────────────────────┐
│  Guest (容器/虚拟机)                     │
│  ┌────────────────────────────────┐    │
│  │  OpenGL/GLES 应用             │    │
│  └───────────┬────────────────────┘    │
│              │ GL Commands             │
│  ┌───────────▼────────────────────┐    │
│  │  Mesa VirGL Driver             │    │
│  └───────────┬────────────────────┘    │
│              │ VirGL Protocol          │
└──────────────┼─────────────────────────┘
               │ vtest Protocol (Socket/Pipe)
┌──────────────▼─────────────────────────┐
│  Host (Android/Linux)                   │
│  ┌───────────────────────────────┐     │
│  │  vtest Server (你要实现的)    │     │
│  └───────────┬───────────────────┘     │
│              │ virglrenderer API       │
│  ┌───────────▼───────────────────┐     │
│  │  virglrenderer Library        │     │
│  └───────────┬───────────────────┘     │
│              │ EGL/OpenGL              │
│  ┌───────────▼───────────────────┐     │
│  │  GPU Driver (Mali/Adreno)     │     │
│  └───────────────────────────────┘     │
└─────────────────────────────────────────┘
```

### 1.2 vtest 协议

**vtest** 是 VirGL 的测试/调试协议，基于简单的 TLV (Type-Length-Value) 格式：

```
┌────────────┬────────────┬──────────────────┐
│  Length    │  Command   │  Data            │
│  (4 bytes) │  (4 bytes) │  (variable)      │
└────────────┴────────────┴──────────────────┘
```

**协议版本**: 4 (VTEST_PROTOCOL_VERSION = 4)

### 1.3 核心组件

- **virglrenderer**: 渲染器核心库，处理 VirGL 命令
- **EGL Manager**: Android EGL 上下文管理
- **vtest Server**: 协议解析和命令分发
- **Callbacks**: EGL 和渲染完成回调

---

## 2. 环境准备

### 2.1 依赖项

```kotlin
// build.gradle.kts (gpu module)
android {
    defaultConfig {
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    // 如果需要 Surface 渲染
    implementation("androidx.core:core-ktx:1.12.0")
}
```

### 2.2 CMake 配置

```cmake
# src/main/cpp/CMakeLists.txt
cmake_minimum_required(VERSION 3.22)
project(virgl_vtest)

# VirGL 渲染器库 (假设已经构建)
add_subdirectory(virglrenderer)

# vtest server 实现
add_library(virgl_vtest SHARED
    vtest_server.cpp
    vtest_io.cpp
    virgl_callbacks.cpp
    virgl_jni.cpp
)

target_include_directories(virgl_vtest PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/virglrenderer/src
)

target_link_libraries(virgl_vtest
    virglrenderer
    EGL
    GLESv3
    android
    log
)
```

### 2.3 必需的头文件

```cpp
// 项目中的关键头文件
#include <virglrenderer.h>              // VirGL 核心 API
#include <virgl_hw.h>                   // 硬件定义
#include <drm/drm-uapi/virtgpu_drm.h>  // DRM 定义
#include <EGL/egl.h>                    // EGL
#include <GLES3/gl3.h>                  // OpenGL ES 3
```

---

## 3. VirGL 初始化

### 3.1 EGL Manager (单例模式)

```cpp
// virgl_egl_manager.h
#ifndef VIRGL_EGL_MANAGER_H
#define VIRGL_EGL_MANAGER_H

#include <EGL/egl.h>
#include <android/native_window.h>

class EGLManager {
public:
    static EGLManager* instance();

    bool initialize();
    EGLContext createContext();
    EGLSurface createPbufferSurface(int width = 1280, int height = 720);
    EGLSurface createWindowSurface(ANativeWindow* window);
    void destroyContext(EGLContext context);
    void destroySurface(EGLSurface surface);
    bool makeCurrent(EGLContext context, EGLSurface surface);
    EGLDisplay getDisplay() const { return display_; }

private:
    EGLManager() = default;
    ~EGLManager();

    EGLDisplay display_ = EGL_NO_DISPLAY;
    EGLConfig config_ = nullptr;
    bool initialized_ = false;
};

#endif
```

```cpp
// virgl_egl_manager.cpp
#include "virgl_egl_manager.h"
#include <android/log.h>

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "EGLManager", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "EGLManager", __VA_ARGS__)

EGLManager* EGLManager::instance() {
    static EGLManager instance;
    return &instance;
}

bool EGLManager::initialize() {
    if (initialized_) {
        return true;
    }

    display_ = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (display_ == EGL_NO_DISPLAY) {
        LOGE("Failed to get EGL display");
        return false;
    }

    EGLint major, minor;
    if (!eglInitialize(display_, &major, &minor)) {
        LOGE("Failed to initialize EGL");
        return false;
    }

    LOGI("EGL version: %d.%d", major, minor);

    // 选择 EGL 配置
    EGLint attribs[] = {
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
        EGL_SURFACE_TYPE, EGL_PBUFFER_BIT | EGL_WINDOW_BIT,
        EGL_RED_SIZE, 8,
        EGL_GREEN_SIZE, 8,
        EGL_BLUE_SIZE, 8,
        EGL_ALPHA_SIZE, 8,
        EGL_DEPTH_SIZE, 24,
        EGL_STENCIL_SIZE, 8,
        EGL_NONE
    };

    EGLint num_configs;
    if (!eglChooseConfig(display_, attribs, &config_, 1, &num_configs)) {
        LOGE("Failed to choose EGL config");
        return false;
    }

    initialized_ = true;
    return true;
}

EGLContext EGLManager::createContext() {
    EGLint ctx_attribs[] = {
        EGL_CONTEXT_CLIENT_VERSION, 3,
        EGL_NONE
    };

    EGLContext ctx = eglCreateContext(display_, config_, EGL_NO_CONTEXT, ctx_attribs);
    if (ctx == EGL_NO_CONTEXT) {
        LOGE("Failed to create EGL context: 0x%x", eglGetError());
        return EGL_NO_CONTEXT;
    }

    return ctx;
}

EGLSurface EGLManager::createPbufferSurface(int width, int height) {
    EGLint pbuffer_attribs[] = {
        EGL_WIDTH, width,
        EGL_HEIGHT, height,
        EGL_NONE
    };

    EGLSurface surface = eglCreatePbufferSurface(display_, config_, pbuffer_attribs);
    if (surface == EGL_NO_SURFACE) {
        LOGE("Failed to create pbuffer surface: 0x%x", eglGetError());
        return EGL_NO_SURFACE;
    }

    return surface;
}

EGLSurface EGLManager::createWindowSurface(ANativeWindow* window) {
    EGLSurface surface = eglCreateWindowSurface(display_, config_, window, nullptr);
    if (surface == EGL_NO_SURFACE) {
        LOGE("Failed to create window surface: 0x%x", eglGetError());
        return EGL_NO_SURFACE;
    }

    return surface;
}

void EGLManager::destroyContext(EGLContext context) {
    if (context != EGL_NO_CONTEXT) {
        eglDestroyContext(display_, context);
    }
}

void EGLManager::destroySurface(EGLSurface surface) {
    if (surface != EGL_NO_SURFACE) {
        eglDestroySurface(display_, surface);
    }
}

bool EGLManager::makeCurrent(EGLContext context, EGLSurface surface) {
    bool success = eglMakeCurrent(display_, surface, surface, context);
    if (!success) {
        LOGE("eglMakeCurrent failed: 0x%x", eglGetError());
    }
    return success;
}

EGLManager::~EGLManager() {
    if (display_ != EGL_NO_DISPLAY) {
        eglTerminate(display_);
    }
}
```

### 3.2 VirGL Callbacks 实现

```cpp
// virgl_callbacks.h
#ifndef VIRGL_CALLBACKS_H
#define VIRGL_CALLBACKS_H

#include <virglrenderer.h>
#include <EGL/egl.h>
#include "virgl_egl_manager.h"

// Server 上下文
struct VirGLServerContext {
    uint32_t ctx_id = 0;
    EGLContext egl_ctx = EGL_NO_CONTEXT;
    EGLSurface egl_surface = EGL_NO_SURFACE;
};

// 1. 获取 EGL Display
static void *get_egl_display_callback(void *cookie) {
    EGLDisplay display = EGLManager::instance()->getDisplay();
    return reinterpret_cast<void *>(display);
}

// 2. 创建 GL Context
static virgl_renderer_gl_context create_gl_context_callback(
        void *cookie, int scanout_idx,
        struct virgl_renderer_gl_ctx_param *param) {

    auto *ctx = static_cast<VirGLServerContext *>(cookie);

    // 创建 EGL Context
    EGLContext egl_ctx = EGLManager::instance()->createContext();
    if (egl_ctx == EGL_NO_CONTEXT) {
        return nullptr;
    }

    // 创建 Pbuffer Surface (离屏渲染)
    EGLSurface surface = EGLManager::instance()->createPbufferSurface(1280, 720);
    if (surface == EGL_NO_SURFACE) {
        EGLManager::instance()->destroyContext(egl_ctx);
        return nullptr;
    }

    // 存储到上下文
    ctx->egl_ctx = egl_ctx;
    ctx->egl_surface = surface;

    return reinterpret_cast<virgl_renderer_gl_context>(egl_ctx);
}

// 3. 销毁 GL Context
static void destroy_gl_context_callback(void *cookie, virgl_renderer_gl_context ctx) {
    EGLContext egl_ctx = reinterpret_cast<EGLContext>(ctx);
    EGLManager::instance()->destroyContext(egl_ctx);
}

// 4. Make Current
static int make_current_callback(void *cookie, int scanout_idx,
                                 virgl_renderer_gl_context ctx) {
    auto *server_ctx = static_cast<VirGLServerContext *>(cookie);

    if (ctx == nullptr) {
        // 解绑上下文
        return EGLManager::instance()->makeCurrent(EGL_NO_CONTEXT, EGL_NO_SURFACE) ? 0 : -EINVAL;
    }

    EGLContext egl_ctx = reinterpret_cast<EGLContext>(ctx);
    bool success = EGLManager::instance()->makeCurrent(egl_ctx, server_ctx->egl_surface);
    return success ? 0 : -EINVAL;
}

// 5. Write Fence (渲染完成通知)
static void write_fence_callback(void *cookie, uint32_t fence_id) {
    // 通知客户端渲染完成
    // 可以通过回调、eventfd 或其他机制通知
    LOGD("Fence %u signaled", fence_id);
}

#endif // VIRGL_CALLBACKS_H
```

### 3.3 初始化 VirGL

```cpp
// virgl_init.cpp
#include "virgl_callbacks.h"
#include <android/log.h>

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "VirGL", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "VirGL", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, "VirGL", __VA_ARGS__)

static VirGLServerContext g_server_ctx;
static bool g_virgl_initialized = false;

bool initialize_virgl() {
    if (g_virgl_initialized) {
        return true;
    }

    // 1. 初始化 EGL
    if (!EGLManager::instance()->initialize()) {
        LOGE("Failed to initialize EGL Manager");
        return false;
    }

    // 2. 设置 VirGL callbacks
    struct virgl_renderer_callbacks callbacks;
    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.version = 4;
    callbacks.get_egl_display = get_egl_display_callback;
    callbacks.create_gl_context = create_gl_context_callback;
    callbacks.destroy_gl_context = destroy_gl_context_callback;
    callbacks.make_current = make_current_callback;
    callbacks.write_fence = write_fence_callback;

    // 3. 初始化 virglrenderer
    int flags = VIRGL_RENDERER_USE_EGL |
                VIRGL_RENDERER_USE_EXTERNAL_BLOB |
                VIRGL_RENDERER_USE_GLES;

    int ret = virgl_renderer_init(&g_server_ctx, flags, &callbacks);
    if (ret != 0) {
        LOGE("virgl_renderer_init failed: %d", ret);
        return false;
    }

    g_virgl_initialized = true;
    LOGI("VirGL initialized successfully");
    return true;
}

void cleanup_virgl() {
    if (g_virgl_initialized) {
        virgl_renderer_cleanup(&g_server_ctx);
        g_virgl_initialized = false;
    }
}
```

---

## 4. vtest 协议详解

### 4.1 消息格式

```cpp
// vtest_protocol.h
#ifndef VTEST_PROTOCOL_H
#define VTEST_PROTOCOL_H

#include <cstdint>

// 协议版本
#define VTEST_PROTOCOL_VERSION 4

// Header 大小 (2 个 uint32_t)
#define VTEST_HDR_SIZE 2

// Header 索引
#define VTEST_CMD_LEN 0  // 数据长度(uint32_t 单位，不含 header)
#define VTEST_CMD_ID  1  // 命令 ID

// 命令定义
enum VTestCommand {
    VCMD_GET_CAPS = 1,
    VCMD_RESOURCE_CREATE = 2,
    VCMD_RESOURCE_UNREF = 3,
    VCMD_TRANSFER_GET = 4,
    VCMD_TRANSFER_PUT = 5,
    VCMD_SUBMIT_CMD = 6,
    VCMD_RESOURCE_BUSY_WAIT = 7,
    VCMD_CREATE_RENDERER = 8,
    VCMD_GET_CAPS2 = 9,
    VCMD_PING_PROTOCOL_VERSION = 10,
    VCMD_PROTOCOL_VERSION = 11,
    VCMD_RESOURCE_CREATE2 = 12,
    VCMD_TRANSFER_GET2 = 13,
    VCMD_TRANSFER_PUT2 = 14,
    VCMD_GET_PARAM = 15,
    VCMD_GET_CAPSET = 16,
    VCMD_CONTEXT_INIT = 17,
    VCMD_RESOURCE_CREATE_BLOB = 18,
    VCMD_SYNC_CREATE = 19,
    VCMD_SYNC_UNREF = 20,
    VCMD_SYNC_READ = 21,
    VCMD_SYNC_WRITE = 22,
    VCMD_SYNC_WAIT = 23,
    VCMD_SUBMIT_CMD2 = 24,
};

// Capset IDs
#define VIRTGPU_DRM_CAPSET_VIRGL 1
#define VIRTGPU_DRM_CAPSET_VIRGL2 2

// Blob 类型
enum VCmdBlobType {
    VCMD_BLOB_TYPE_GUEST = 1,
    VCMD_BLOB_TYPE_HOST3D = 2,
    VCMD_BLOB_TYPE_HOST3D_GUEST = 3,
};

// Blob 标志
enum VCmdBlobFlag {
    VCMD_BLOB_FLAG_MAPPABLE = 1 << 0,
    VCMD_BLOB_FLAG_SHAREABLE = 1 << 1,
    VCMD_BLOB_FLAG_CROSS_DEVICE = 1 << 2,
};

// Header 结构
struct VTestHeader {
    uint32_t length;  // 数据长度 (uint32_t 单位)
    uint32_t cmd_id;  // 命令 ID
};

#endif // VTEST_PROTOCOL_H
```

### 4.2 I/O 工具类

```cpp
// vtest_io.h
#ifndef VTEST_IO_H
#define VTEST_IO_H

#include "vtest_protocol.h"
#include <vector>

class VTestIO {
public:
    explicit VTestIO(int fd);

    // 读取消息头
    bool readHeader(VTestHeader &header);

    // 读取消息数据
    bool readData(void *data, size_t len);

    // 读取数据到 vector
    bool readData(std::vector<uint32_t> &data, uint32_t count);

    // 写入响应
    bool writeResponse(uint32_t cmd_id, const void *data, size_t len);

    // 写入响应 (从 vector)
    bool writeResponse(uint32_t cmd_id, const std::vector<uint32_t> &data);

    // 获取文件描述符
    int getFd() const { return fd_; }

private:
    int fd_;

    ssize_t readFull(void *buf, size_t len);
    ssize_t writeFull(const void *buf, size_t len);
};

#endif // VTEST_IO_H
```

```cpp
// vtest_io.cpp
#include "vtest_io.h"
#include <unistd.h>
#include <android/log.h>

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "VTestIO", __VA_ARGS__)

VTestIO::VTestIO(int fd) : fd_(fd) {}

bool VTestIO::readHeader(VTestHeader &header) {
    uint32_t buf[VTEST_HDR_SIZE];
    if (readFull(buf, sizeof(buf)) != sizeof(buf)) {
        LOGE("Failed to read header");
        return false;
    }

    header.length = buf[VTEST_CMD_LEN];
    header.cmd_id = buf[VTEST_CMD_ID];
    return true;
}

bool VTestIO::readData(void *data, size_t len) {
    return readFull(data, len) == static_cast<ssize_t>(len);
}

bool VTestIO::readData(std::vector<uint32_t> &data, uint32_t count) {
    data.resize(count);
    return readData(data.data(), count * sizeof(uint32_t));
}

bool VTestIO::writeResponse(uint32_t cmd_id, const void *data, size_t len) {
    uint32_t header[VTEST_HDR_SIZE];
    header[VTEST_CMD_LEN] = len / sizeof(uint32_t);
    header[VTEST_CMD_ID] = cmd_id;

    if (writeFull(header, sizeof(header)) != sizeof(header)) {
        LOGE("Failed to write response header");
        return false;
    }

    if (len > 0 && writeFull(data, len) != static_cast<ssize_t>(len)) {
        LOGE("Failed to write response data");
        return false;
    }

    return true;
}

bool VTestIO::writeResponse(uint32_t cmd_id, const std::vector<uint32_t> &data) {
    return writeResponse(cmd_id, data.data(), data.size() * sizeof(uint32_t));
}

ssize_t VTestIO::readFull(void *buf, size_t len) {
    size_t total = 0;
    while (total < len) {
        ssize_t n = read(fd_, static_cast<char *>(buf) + total, len - total);
        if (n <= 0) {
            if (n == 0) {
                LOGE("Connection closed");
            } else {
                LOGE("Read error: %d", errno);
            }
            return n;
        }
        total += n;
    }
    return total;
}

ssize_t VTestIO::writeFull(const void *buf, size_t len) {
    size_t total = 0;
    while (total < len) {
        ssize_t n = write(fd_, static_cast<const char *>(buf) + total, len - total);
        if (n <= 0) {
            LOGE("Write error: %d", errno);
            return n;
        }
        total += n;
    }
    return total;
}
```

---

## 5. 实现 Server 核心

### 5.1 Server 类定义

```cpp
// vtest_server.h
#ifndef VTEST_SERVER_H
#define VTEST_SERVER_H

#include "vtest_io.h"
#include "vtest_protocol.h"
#include <unordered_map>

class VTestServer {
public:
    explicit VTestServer(int client_fd);
    ~VTestServer();

    // 运行服务器主循环
    bool run();

    // 停止服务器
    void stop();

private:
    VTestIO io_;
    uint32_t ctx_id_;
    bool running_;

    // 资源管理
    std::unordered_map<uint32_t, uint32_t> resources_;  // res_id -> handle

    // 命令处理
    bool handleCommand(const VTestHeader &header);

    // 各命令处理函数
    bool handleProtocolVersion(const VTestHeader &header);
    bool handleGetCaps2(const VTestHeader &header);
    bool handleGetCapset(const VTestHeader &header);
    bool handleContextInit(const VTestHeader &header);
    bool handleResourceCreateBlob(const VTestHeader &header);
    bool handleResourceUnref(const VTestHeader &header);
    bool handleSubmitCmd(const VTestHeader &header);
    bool handleTransferPut(const VTestHeader &header);
    bool handleTransferGet(const VTestHeader &header);
};

#endif // VTEST_SERVER_H
```

### 5.2 Server 实现

```cpp
// vtest_server.cpp
#include "vtest_server.h"
#include <virglrenderer.h>
#include <android/log.h>

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "VTestServer", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "VTestServer", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, "VTestServer", __VA_ARGS__)

VTestServer::VTestServer(int client_fd)
    : io_(client_fd), ctx_id_(0), running_(false) {
}

VTestServer::~VTestServer() {
    stop();
}

bool VTestServer::run() {
    LOGI("vtest server started, fd=%d", io_.getFd());
    running_ = true;

    while (running_) {
        VTestHeader header;
        if (!io_.readHeader(header)) {
            LOGE("Failed to read header, stopping");
            break;
        }

        LOGD("Command: %u, length: %u", header.cmd_id, header.length);

        if (!handleCommand(header)) {
            LOGE("Failed to handle command %u", header.cmd_id);
            break;
        }
    }

    LOGI("vtest server stopped");
    return true;
}

void VTestServer::stop() {
    running_ = false;

    // 清理资源
    for (auto &pair : resources_) {
        virgl_renderer_resource_unref(pair.second);
    }
    resources_.clear();

    // 销毁上下文
    if (ctx_id_ != 0) {
        virgl_renderer_context_destroy(ctx_id_);
        ctx_id_ = 0;
    }
}

bool VTestServer::handleCommand(const VTestHeader &header) {
    switch (header.cmd_id) {
        case VCMD_PROTOCOL_VERSION:
            return handleProtocolVersion(header);

        case VCMD_GET_CAPS2:
            return handleGetCaps2(header);

        case VCMD_GET_CAPSET:
            return handleGetCapset(header);

        case VCMD_CONTEXT_INIT:
            return handleContextInit(header);

        case VCMD_RESOURCE_CREATE_BLOB:
            return handleResourceCreateBlob(header);

        case VCMD_RESOURCE_UNREF:
            return handleResourceUnref(header);

        case VCMD_SUBMIT_CMD:
            return handleSubmitCmd(header);

        case VCMD_TRANSFER_PUT2:
            return handleTransferPut(header);

        case VCMD_TRANSFER_GET2:
            return handleTransferGet(header);

        default:
            LOGW("Unhandled command: %u", header.cmd_id);
            // 读取并丢弃数据
            if (header.length > 0) {
                std::vector<uint32_t> dummy(header.length);
                io_.readData(dummy, header.length);
            }
            return true;
    }
}

// ============ 命令实现 ============

bool VTestServer::handleProtocolVersion(const VTestHeader &header) {
    uint32_t client_version;
    if (!io_.readData(&client_version, sizeof(client_version))) {
        return false;
    }

    LOGI("Client protocol version: %u", client_version);

    uint32_t server_version = VTEST_PROTOCOL_VERSION;
    return io_.writeResponse(VCMD_PROTOCOL_VERSION, &server_version, sizeof(server_version));
}

bool VTestServer::handleGetCaps2(const VTestHeader &header) {
    uint32_t params[2];
    if (!io_.readData(params, sizeof(params))) {
        return false;
    }

    uint32_t capset_id = params[0];
    uint32_t capset_version = params[1];

    LOGD("GET_CAPS2: capset_id=%u, version=%u", capset_id, capset_version);

    // 获取 capabilities
    uint32_t max_ver, max_size;
    virgl_renderer_get_cap_set(capset_id, &max_ver, &max_size);

    if (max_size == 0) {
        LOGW("No capabilities for capset %u", capset_id);
        uint32_t valid = 0;
        return io_.writeResponse(VCMD_GET_CAPS2, &valid, sizeof(valid));
    }

    std::vector<uint8_t> caps_data(max_size + sizeof(uint32_t));
    uint32_t *response = reinterpret_cast<uint32_t *>(caps_data.data());
    response[0] = 1;  // valid

    virgl_renderer_fill_caps(capset_id, max_ver, &response[1]);

    return io_.writeResponse(VCMD_GET_CAPS2, caps_data.data(), caps_data.size());
}

bool VTestServer::handleGetCapset(const VTestHeader &header) {
    uint32_t params[2];
    if (!io_.readData(params, sizeof(params))) {
        return false;
    }

    uint32_t capset_id = params[0];
    uint32_t capset_version = params[1];

    LOGD("GET_CAPSET: id=%u, version=%u", capset_id, capset_version);

    uint32_t max_ver, max_size;
    virgl_renderer_get_cap_set(capset_id, &max_ver, &max_size);

    if (max_size == 0) {
        uint32_t valid = 0;
        return io_.writeResponse(VCMD_GET_CAPSET, &valid, sizeof(valid));
    }

    std::vector<uint8_t> caps_data(max_size + sizeof(uint32_t));
    uint32_t *response = reinterpret_cast<uint32_t *>(caps_data.data());
    response[0] = 1;  // valid

    virgl_renderer_fill_caps(capset_id, max_ver, &response[1]);

    return io_.writeResponse(VCMD_GET_CAPSET, caps_data.data(), caps_data.size());
}

bool VTestServer::handleContextInit(const VTestHeader &header) {
    uint32_t capset_id;
    if (!io_.readData(&capset_id, sizeof(capset_id))) {
        return false;
    }

    LOGI("CONTEXT_INIT: capset_id=%u", capset_id);

    // 创建 virgl 上下文
    ctx_id_ = 1;  // 使用固定上下文 ID
    int ret = virgl_renderer_context_create_with_flags(
        ctx_id_, capset_id, strlen("vtest"), "vtest");

    if (ret != 0) {
        LOGE("virgl_renderer_context_create_with_flags failed: %d", ret);
        return false;
    }

    LOGI("VirGL context %u created successfully", ctx_id_);
    return true;  // 无响应
}

bool VTestServer::handleResourceCreateBlob(const VTestHeader &header) {
    uint32_t params[6];
    if (!io_.readData(params, sizeof(params))) {
        return false;
    }

    uint32_t blob_type = params[0];
    uint32_t blob_flags = params[1];
    uint64_t size = (static_cast<uint64_t>(params[3]) << 32) | params[2];
    uint64_t blob_id = (static_cast<uint64_t>(params[5]) << 32) | params[4];

    LOGD("RESOURCE_CREATE_BLOB: type=%u, flags=0x%x, size=%llu, id=%llu",
         blob_type, blob_flags, (unsigned long long)size, (unsigned long long)blob_id);

    struct virgl_renderer_resource_create_blob_args args = {};
    args.res_handle = 0;  // virgl 会分配
    args.ctx_id = ctx_id_;
    args.blob_mem = blob_type;
    args.blob_flags = blob_flags;
    args.size = size;
    args.blob_id = blob_id;

    int ret = virgl_renderer_resource_create_blob(&args);
    if (ret != 0) {
        LOGE("virgl_renderer_resource_create_blob failed: %d", ret);
        return false;
    }

    uint32_t res_id = args.res_handle;
    resources_[res_id] = res_id;

    LOGD("Resource created: res_id=%u", res_id);
    return io_.writeResponse(VCMD_RESOURCE_CREATE_BLOB, &res_id, sizeof(res_id));
}

bool VTestServer::handleResourceUnref(const VTestHeader &header) {
    uint32_t res_id;
    if (!io_.readData(&res_id, sizeof(res_id))) {
        return false;
    }

    LOGD("RESOURCE_UNREF: res_id=%u", res_id);

    auto it = resources_.find(res_id);
    if (it != resources_.end()) {
        virgl_renderer_resource_unref(it->second);
        resources_.erase(it);
    }

    return true;  // 无响应
}

bool VTestServer::handleSubmitCmd(const VTestHeader &header) {
    std::vector<uint32_t> cmd_buf;
    if (!io_.readData(cmd_buf, header.length)) {
        return false;
    }

    LOGD("SUBMIT_CMD: size=%u dwords", header.length);

    int ret = virgl_renderer_submit_cmd(cmd_buf.data(), ctx_id_, header.length);
    if (ret != 0) {
        LOGE("virgl_renderer_submit_cmd failed: %d", ret);
        return false;
    }

    return true;  // 无响应
}

bool VTestServer::handleTransferPut(const VTestHeader &header) {
    uint32_t params[10];
    if (!io_.readData(params, sizeof(params))) {
        return false;
    }

    uint32_t res_id = params[0];
    uint32_t level = params[1];
    uint32_t x = params[2];
    uint32_t y = params[3];
    uint32_t z = params[4];
    uint32_t width = params[5];
    uint32_t height = params[6];
    uint32_t depth = params[7];
    uint32_t data_size = params[8];
    uint64_t offset = (static_cast<uint64_t>(params[9]) << 32) | data_size;

    LOGD("TRANSFER_PUT2: res=%u, level=%u, box=(%u,%u,%u,%u,%u,%u), size=%u",
         res_id, level, x, y, z, width, height, depth, data_size);

    // 读取数据
    std::vector<uint8_t> data(data_size);
    if (data_size > 0 && !io_.readData(data.data(), data_size)) {
        return false;
    }

    // 传输到 virgl
    struct iovec iov = { data.data(), data_size };
    struct virgl_box box = { x, y, z, width, height, depth };

    int ret = virgl_renderer_transfer_write_iov(
        res_id, ctx_id_, level, 0, 0, &box, offset, &iov, 1);

    if (ret != 0) {
        LOGE("virgl_renderer_transfer_write_iov failed: %d", ret);
        return false;
    }

    return true;  // 无响应
}

bool VTestServer::handleTransferGet(const VTestHeader &header) {
    uint32_t params[10];
    if (!io_.readData(params, sizeof(params))) {
        return false;
    }

    uint32_t res_id = params[0];
    uint32_t level = params[1];
    uint32_t x = params[2];
    uint32_t y = params[3];
    uint32_t z = params[4];
    uint32_t width = params[5];
    uint32_t height = params[6];
    uint32_t depth = params[7];
    uint32_t data_size = params[8];
    uint64_t offset = (static_cast<uint64_t>(params[9]) << 32) | data_size;

    LOGD("TRANSFER_GET2: res=%u, level=%u, box=(%u,%u,%u,%u,%u,%u), size=%u",
         res_id, level, x, y, z, width, height, depth, data_size);

    // 准备接收缓冲区
    std::vector<uint8_t> data(data_size);
    struct iovec iov = { data.data(), data_size };
    struct virgl_box box = { x, y, z, width, height, depth };

    int ret = virgl_renderer_transfer_read_iov(
        res_id, ctx_id_, level, 0, 0, &box, offset, &iov, 1);

    if (ret != 0) {
        LOGE("virgl_renderer_transfer_read_iov failed: %d", ret);
        return false;
    }

    // 发送数据回客户端
    return io_.writeResponse(VCMD_TRANSFER_GET2, data.data(), data.size());
}
```

---

## 6. 完整示例代码

### 6.1 JNI 接口

```kotlin
// VirGLServer.kt
package com.github.andock.gpu.virgl

object VirGLServer {

    init {
        System.loadLibrary("virgl_vtest")
    }

    /**
     * 初始化 VirGL 渲染器 (全局初始化，只需调用一次)
     */
    external fun initVirGL(): Int

    /**
     * 启动 vtest server
     * @param clientFd 客户端连接的文件描述符
     * @return 0 on success
     */
    external fun startServer(clientFd: Int): Int

    /**
     * 停止 vtest server
     */
    external fun stopServer()
}
```

### 6.2 JNI 实现

```cpp
// virgl_jni.cpp
#include <jni.h>
#include "vtest_server.h"
#include "virgl_init.h"

static std::unique_ptr<VTestServer> g_server;

extern "C"
JNIEXPORT jint JNICALL
Java_com_github_andock_gpu_virgl_VirGLServer_initVirGL(JNIEnv *env, jclass) {
    return initialize_virgl() ? 0 : -1;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_github_andock_gpu_virgl_VirGLServer_startServer(
        JNIEnv *env, jclass, jint clientFd) {

    // 创建并运行 server
    g_server = std::make_unique<VTestServer>(clientFd);
    bool success = g_server->run();

    return success ? 0 : -1;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_github_andock_gpu_virgl_VirGLServer_stopServer(JNIEnv *env, jclass) {
    if (g_server) {
        g_server->stop();
        g_server.reset();
    }
}
```

### 6.3 Android 使用示例

```kotlin
// MainActivity.kt
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.appcompat.app.AppCompatActivity
import com.github.andock.gpu.virgl.VirGLServer
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 初始化 VirGL
        val ret = VirGLServer.initVirGL()
        if (ret != 0) {
            Log.e(TAG, "Failed to initialize VirGL: $ret")
            return
        }

        // 2. 创建 Unix Socket 连接 (示例)
        val socketPath = File(filesDir, "virgl.sock")
        val clientFd = createUnixSocketClient(socketPath)

        // 3. 启动 server (在后台线程)
        Thread {
            val result = VirGLServer.startServer(clientFd.fd)
            Log.i(TAG, "Server exited with code: $result")
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        VirGLServer.stopServer()
    }

    companion object {
        const val TAG = "VirGLDemo"
    }
}
```

---

## 7. 调试与测试

### 7.1 启用 VirGL 日志

```cpp
// 在 initialize_virgl() 之前调用
virgl_set_log_callback([](enum virgl_log_level_flags level,
                          const char *message, void *user_data) {
    const char *level_str;
    int android_log_level;

    switch (level) {
        case VIRGL_LOG_LEVEL_DEBUG:
            level_str = "DEBUG";
            android_log_level = ANDROID_LOG_DEBUG;
            break;
        case VIRGL_LOG_LEVEL_INFO:
            level_str = "INFO";
            android_log_level = ANDROID_LOG_INFO;
            break;
        case VIRGL_LOG_LEVEL_WARNING:
            level_str = "WARN";
            android_log_level = ANDROID_LOG_WARN;
            break;
        case VIRGL_LOG_LEVEL_ERROR:
            level_str = "ERROR";
            android_log_level = ANDROID_LOG_ERROR;
            break;
        default:
            level_str = "UNKNOWN";
            android_log_level = ANDROID_LOG_INFO;
            break;
    }

    __android_log_print(android_log_level, "VirGL", "[%s] %s", level_str, message);
}, nullptr, nullptr);
```

### 7.2 测试客户端

使用 virglrenderer 自带的 vtest 客户端:

```bash
# 在容器中
export GALLIUM_DRIVER=virpipe
export VIRGL_RENDERER_SOCKET_PATH=/path/to/socket

# 测试简单 OpenGL 程序
glxinfo
glxgears
```

### 7.3 调试技巧

1. **检查 EGL 错误**:
```cpp
EGLint error = eglGetError();
if (error != EGL_SUCCESS) {
    LOGE("EGL error: 0x%x", error);
}
```

2. **验证上下文创建**:
```cpp
EGLContext ctx = eglGetCurrentContext();
LOGD("Current context: %p", ctx);
```

3. **监控资源使用**:
```cpp
LOGD("Active resources: %zu", resources_.size());
```

4. **使用 adb logcat 过滤**:
```bash
adb logcat -s VirGL:* VTestServer:* VTestIO:* EGLManager:*
```

### 7.4 常见问题

**问题 1: EGL 初始化失败**
```
解决方案:
- 确保在有 GL 能力的线程调用
- 检查设备是否支持 OpenGL ES 3.0+
```

**问题 2: 渲染无输出**
```
解决方案:
- 确保 make_current 成功
- 检查 fence callback 是否被调用
- 验证 Surface 是否正确创建
```

**问题 3: 性能问题**
```
解决方案:
- 使用 VIRGL_RENDERER_THREAD_SYNC 标志
- 考虑使用 Window Surface 代替 Pbuffer
- 检查是否有频繁的上下文切换
```

**问题 4: 内存泄漏**
```
解决方案:
- 确保所有资源都被正确释放
- 使用 RAII 包装 EGL 对象
- 在 server 停止时清理所有 virgl 资源
```

---

## 8. Android 集成

### 8.1 权限配置

```xml
<!-- AndroidManifest.xml -->
<manifest>
    <!-- 如果需要访问外部存储 -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

    <!-- OpenGL ES 3.0 要求 -->
    <uses-feature android:glEsVersion="0x00030000" android:required="true" />
</manifest>
```

### 8.2 ProGuard 规则

```proguard
# virgl-rules.pro
-keep class com.github.andock.gpu.virgl.VirGLServer {
    native <methods>;
}

-keep class com.github.andock.gpu.virgl.VirGLRenderer {
    void onFenceSignaled(int);
}
```

### 8.3 多线程注意事项

VirGL 的 EGL 上下文绑定到线程，确保:

1. **Server 在独立线程运行**
```kotlin
Thread {
    VirGLServer.startServer(fd)
}.start()
```

2. **不要跨线程调用 EGL**
```cpp
// 错误示例: 在不同线程使用同一个 context
Thread A: eglMakeCurrent(ctx, surface)
Thread B: glDrawArrays()  // 崩溃!

// 正确示例: 每个线程独立的 context
Thread A: eglMakeCurrent(ctx_a, surface_a)
Thread B: eglMakeCurrent(ctx_b, surface_b)
```

3. **Fence callback 可能在任意线程**
```cpp
static void write_fence_callback(void *cookie, uint32_t fence_id) {
    // 如果需要回调 Java 层，确保正确获取 JNIEnv
    JNIEnv *env = getJNIEnv();  // 线程本地
    // ...
}
```

---

## 总结

本教程涵盖了:

1. **VirGL 架构** - 理解 GPU 虚拟化工作原理
2. **EGL 管理** - Android EGL 初始化和上下文管理
3. **Callbacks 实现** - virglrenderer 所需的回调函数
4. **vtest 协议** - 消息格式和命令处理
5. **Server 实现** - 完整的 vtest server 代码
6. **Android 集成** - JNI 绑定和使用示例
7. **调试技巧** - 日志、错误处理和常见问题

## 参考资源

- **virglrenderer 源码**: `gpu/src/main/cpp/virglrenderer/`
- **vtest 协议定义**: `virglrenderer/vtest/vtest_protocol.h`
- **项目示例代码**:
  - `gpu/src/main/cpp/virgl_jni.cpp`
  - `gpu/src/main/java/com/github/andock/gpu/virgl/VirGLNative.kt`
- **官方文档**: https://gitlab.freedesktop.org/virgl/virglrenderer

## 下一步

- 实现更多 vtest 命令 (SYNC, DRM_SYNC 系列)
- 优化性能 (异步渲染、共享内存)
- 支持多客户端连接
- 添加 GPU 性能监控
