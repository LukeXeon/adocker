# VirGL 架构详解

## 目录

1. [核心概念](#1-核心概念)
2. [virgl_renderer_gl_context 详解](#2-virgl_renderer_gl_context-详解)
3. [virgl_context 详解](#3-virgl_context-详解)
4. [两者关系](#4-两者关系)
5. [工作流程](#5-工作流程)
6. [实际应用场景](#6-实际应用场景)
7. [最佳实践](#7-最佳实践)

---

## 1. 核心概念

### 1.1 VirGL 是什么？

VirGL (Virtual OpenGL) 是一个 GPU 虚拟化解决方案，它包含两个核心抽象层：

```
┌─────────────────────────────────────────────────────────┐
│  应用层 (Android App)                                    │
│  ┌───────────────────────────────────────────────┐     │
│  │  EGLContext (真实的 OpenGL 上下文)             │     │
│  │  由 eglCreateContext() 创建                    │     │
│  └─────────────┬─────────────────────────────────┘     │
│                │ 包装为不透明指针                        │
│  ┌─────────────▼─────────────────────────────────┐     │
│  │  virgl_renderer_gl_context (外部 API 层)      │     │
│  │  typedef void *virgl_renderer_gl_context;     │     │
│  └─────────────┬─────────────────────────────────┘     │
└────────────────┼──────────────────────────────────────┘
                 │ 通过 callbacks 传递
┌────────────────▼──────────────────────────────────────┐
│  virglrenderer 库内部                                  │
│  ┌─────────────────────────────────────────────┐     │
│  │  virgl_context (内部逻辑层)                  │     │
│  │  struct virgl_context {                     │     │
│  │    uint32_t ctx_id;                         │     │
│  │    int (*submit_cmd)(...);                  │     │
│  │    int (*transfer_3d)(...);                 │     │
│  │  };                                         │     │
│  └─────────────┬───────────────────────────────┘     │
│                │ 继承/实现                             │
│  ┌─────────────▼───────────────────────────────┐     │
│  │  vrend_decode_ctx (VirGL 渲染器具体实现)     │     │
│  │  或 venus_context (Vulkan 实现)             │     │
│  └─────────────────────────────────────────────┘     │
└──────────────────────────────────────────────────────┘
```

### 1.2 为什么需要两层抽象？

**问题**: GPU 虚拟化需要在客户机（容器/虚拟机）和宿主机之间共享 GPU

**解决方案**: 分离关注点
1. **外部层** (`virgl_renderer_gl_context`): 处理真实的 GL 上下文，由宿主应用管理
2. **内部层** (`virgl_context`): 处理虚拟化逻辑，由 virglrenderer 管理

---

## 2. virgl_renderer_gl_context 详解

### 2.1 定义

```c
// virglrenderer/src/virglrenderer.h:41
typedef void *virgl_renderer_gl_context;
```

**本质**: 不透明指针 (`void *`)，实际上指向 `EGLContext`

### 2.2 生命周期

```cpp
// 创建 - 通过 callback
static virgl_renderer_gl_context create_gl_context_callback(
        void *cookie, int scanout_idx,
        struct virgl_renderer_gl_ctx_param *param) {

    // 1. 创建 EGL 上下文
    EGLint ctx_attribs[] = {
        EGL_CONTEXT_CLIENT_VERSION, param->major_ver,
        EGL_NONE
    };

    EGLContext egl_ctx = eglCreateContext(
        display, config,
        param->shared ? eglGetCurrentContext() : EGL_NO_CONTEXT,
        ctx_attribs
    );

    // 2. 创建 Surface (重要!)
    EGLSurface surface = eglCreatePbufferSurface(display, config, pbuffer_attribs);

    // 3. 存储 Surface 到 cookie (稍后 make_current 需要)
    auto *ctx = static_cast<VirGLServerContext *>(cookie);
    ctx->egl_surface = surface;
    ctx->egl_ctx = egl_ctx;

    // 4. 返回为不透明指针
    return reinterpret_cast<virgl_renderer_gl_context>(egl_ctx);
}

// 激活 - 通过 callback
static int make_current_callback(void *cookie, int scanout_idx,
                                 virgl_renderer_gl_context ctx) {
    auto *server_ctx = static_cast<VirGLServerContext *>(cookie);

    if (ctx == nullptr) {
        // 解绑上下文
        eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        return 0;
    }

    // 转换回 EGLContext
    EGLContext egl_ctx = reinterpret_cast<EGLContext>(ctx);

    // 使用存储的 Surface
    EGLSurface surface = server_ctx->egl_surface;

    bool success = eglMakeCurrent(display, surface, surface, egl_ctx);
    return success ? 0 : -EINVAL;
}

// 销毁 - 通过 callback
static void destroy_gl_context_callback(void *cookie,
                                        virgl_renderer_gl_context ctx) {
    EGLContext egl_ctx = reinterpret_cast<EGLContext>(ctx);

    // 确保未激活
    EGLContext current = eglGetCurrentContext();
    if (current == egl_ctx) {
        eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    }

    // 销毁
    eglDestroyContext(display, egl_ctx);

    // 注意: 也需要销毁 Surface (在适当的时机)
    // eglDestroySurface(display, server_ctx->egl_surface);
}
```

### 2.3 特性

| 特性 | 说明 |
|------|------|
| **类型** | `void *` - 不透明指针 |
| **实际指向** | `EGLContext` (Android/Linux) 或 `HGLRC` (Windows) |
| **管理者** | 应用层 (你的代码) |
| **线程绑定** | 是 - EGL 上下文绑定到线程 |
| **数量** | 可能有多个 (每个 scanout/显示器一个) |
| **作用域** | 外部 API 层 |

### 2.4 重要注意事项

**必须管理 EGLSurface:**
```cpp
// ❌ 错误: 只创建 Context，没有 Surface
EGLContext ctx = eglCreateContext(...);
return (virgl_renderer_gl_context)ctx;

// ✅ 正确: 同时创建 Context 和 Surface
EGLContext ctx = eglCreateContext(...);
EGLSurface surface = eglCreatePbufferSurface(...);
// 存储 surface 到 cookie，make_current 时使用
server_ctx->egl_surface = surface;
return (virgl_renderer_gl_context)ctx;
```

**线程安全:**
```cpp
// EGL 上下文绑定到线程
Thread A: eglMakeCurrent(ctx, surface)  // ctx 绑定到 Thread A
Thread B: glDrawArrays()                // ❌ 错误! 未绑定上下文

// 正确做法
Thread B: eglMakeCurrent(ctx, surface)  // 先绑定到 Thread B
Thread B: glDrawArrays()                // ✅ 正确
```

---

## 3. virgl_context 详解

### 3.1 定义

```c
// virglrenderer/src/virgl_context.h:64
struct virgl_context {
    uint32_t ctx_id;              // 上下文 ID (唯一标识)
    uint32_t capset_id;           // 能力集 ID (VIRGL2, VENUS, etc.)
    int in_fence_fd;              // 输入 Fence FD

    // Fence 回调
    virgl_context_fence_retire fence_retire;
    bool supports_fence_sharing;

    // 生命周期方法
    void (*destroy)(struct virgl_context *ctx);

    // 资源管理方法
    void (*attach_resource)(struct virgl_context *ctx,
                           struct virgl_resource *res);
    void (*detach_resource)(struct virgl_context *ctx,
                           struct virgl_resource *res);

    // 数据传输方法
    int (*transfer_3d)(struct virgl_context *ctx,
                      struct virgl_resource *res,
                      const struct vrend_transfer_info *info,
                      int transfer_mode);

    // Blob 资源方法
    int (*get_blob)(struct virgl_context *ctx,
                   uint32_t res_id,
                   uint64_t blob_id,
                   uint64_t blob_size,
                   uint32_t blob_flags,
                   struct virgl_context_blob *blob);

    // 命令提交方法
    int (*submit_cmd)(struct virgl_context *ctx,
                     const void *buffer,
                     size_t size);

    // Fence 管理方法
    int (*get_fencing_fd)(struct virgl_context *ctx);
    void (*retire_fences)(struct virgl_context *ctx);
    int (*submit_fence)(struct virgl_context *ctx,
                       uint32_t flags,
                       uint32_t ring_idx,
                       uint64_t fence_id);
};
```

### 3.2 生命周期

```c
// 创建 - 通过 virglrenderer API
int virgl_renderer_context_create_with_flags(
        uint32_t ctx_id,
        uint32_t ctx_flags,  // 实际上是 capset_id
        uint32_t nlen,
        const char *name) {

    uint32_t capset_id = ctx_flags & VIRGL_RENDERER_CONTEXT_FLAG_CAPSET_ID_MASK;
    struct virgl_context *ctx = NULL;

    // 根据 capset_id 创建不同的实现
    switch (capset_id) {
        case VIRTGPU_DRM_CAPSET_VIRGL:
        case VIRTGPU_DRM_CAPSET_VIRGL2:
            // VirGL 渲染器实现
            ctx = vrend_create_context(ctx_id, capset_id, nlen, name);
            break;

        case VIRTGPU_DRM_CAPSET_VENUS:
            // Venus (Vulkan) 实现
            ctx = venus_create_context(ctx_id, capset_id, nlen, name);
            break;

        default:
            return -EINVAL;
    }

    if (!ctx) {
        return -ENOMEM;
    }

    // 设置 Fence 回调
    ctx->fence_retire = per_context_fence_retire;

    // 添加到全局上下文表
    virgl_context_add(ctx);

    return 0;
}

// 使用 - 提交命令
int virgl_renderer_submit_cmd(void *buffer, int ctx_id, int ndw) {
    // 查找上下文
    struct virgl_context *ctx = virgl_context_lookup(ctx_id);
    if (!ctx) {
        return -EINVAL;
    }

    // 调用上下文方法
    return ctx->submit_cmd(ctx, buffer, ndw * sizeof(uint32_t));
}

// 销毁 - 通过 virglrenderer API
void virgl_renderer_context_destroy(uint32_t ctx_id) {
    struct virgl_context *ctx = virgl_context_lookup(ctx_id);
    if (!ctx) {
        return;
    }

    // 从全局表移除
    virgl_context_remove(ctx_id);

    // 调用销毁方法
    if (ctx->destroy) {
        ctx->destroy(ctx);
    }
}
```

### 3.3 特性

| 特性 | 说明 |
|------|------|
| **类型** | `struct` - 完整结构体 |
| **管理者** | virglrenderer 内部 |
| **线程绑定** | 否 - 逻辑结构，线程无关 |
| **数量** | 每个 ctx_id 一个 |
| **作用域** | 内部实现层 |
| **多态性** | 通过函数指针实现 (类似虚函数表) |

### 3.4 继承体系

```
struct virgl_context (基类)
    │
    ├─ vrend_decode_ctx (VirGL 实现)
    │   └─ 使用 OpenGL/GLES 渲染
    │
    ├─ venus_context (Venus/Vulkan 实现)
    │   └─ 使用 Vulkan 渲染
    │
    └─ drm_context (DRM 实现)
        └─ 直接使用 DRM API
```

---

## 4. 两者关系

### 4.1 关键区别对比

| 维度 | virgl_renderer_gl_context | virgl_context |
|------|---------------------------|---------------|
| **定义层次** | 外部 API 层 | 内部实现层 |
| **类型** | `void *` (不透明指针) | `struct` (结构体) |
| **代表** | 真实的 EGL/OpenGL 上下文 | VirGL 逻辑渲染上下文 |
| **管理者** | 应用层 (你的代码) | virglrenderer 库 |
| **创建方式** | callback: `create_gl_context()` | API: `virgl_renderer_context_create()` |
| **销毁方式** | callback: `destroy_gl_context()` | API: `virgl_renderer_context_destroy()` |
| **线程绑定** | 是 (EGL 要求) | 否 (逻辑结构) |
| **数量关系** | 可能多个 (每个 scanout) | 每个 ctx_id 一个 |
| **存储位置** | 应用的数据结构 | virglrenderer 内部哈希表 |
| **生命周期** | 跨越整个应用 | 仅在上下文使用期间 |

### 4.2 交互模型

```
应用层:
  EGLContext egl_ctx_0 = eglCreateContext(...);  // 主显示器
  EGLContext egl_ctx_1 = eglCreateContext(...);  // 副显示器
        │                      │
        │ 转换为 void*          │
        │                      │
        ▼                      ▼
  virgl_renderer_gl_context   virgl_renderer_gl_context
  (scanout_idx=0)            (scanout_idx=1)
        │                      │
        └──────────┬───────────┘
                   │ 通过 make_current 使用
                   │
virglrenderer 内部:
                   ▼
         struct virgl_context
         {
           ctx_id: 1,
           capset_id: VIRGL2,
           submit_cmd: vrend_submit_cmd,
           ...
         }
```

### 4.3 依赖关系

**virgl_context 依赖 virgl_renderer_gl_context:**

```c
// 当 virgl_context 需要执行 GL 命令时
struct virgl_context *ctx = virgl_context_lookup(1);

// 内部会调用 make_current callback
make_current_callback(cookie, scanout_idx, stored_gl_context);

// 然后执行 GL 命令
glClear(GL_COLOR_BUFFER_BIT);
glDrawArrays(GL_TRIANGLES, 0, 3);
```

**数量关系:**
- **1 对 0**: 如果使用 virglrenderer 内部 winsys (不需要 callbacks)
- **1 对 1**: 最常见，1 个 virgl_context 使用 1 个 GL 上下文
- **1 对 多**: 多显示器场景，1 个 virgl_context 可能切换使用多个 GL 上下文

---

## 5. 工作流程

### 5.1 初始化流程

```cpp
// 1. 应用初始化 EGL
EGLDisplay display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
eglInitialize(display, &major, &minor);

// 2. 设置 callbacks
struct virgl_renderer_callbacks cbs = {
    .version = 4,
    .get_egl_display = get_egl_display_callback,
    .create_gl_context = create_gl_context_callback,
    .destroy_gl_context = destroy_gl_context_callback,
    .make_current = make_current_callback,
    .write_fence = write_fence_callback,
};

// 3. 初始化 virglrenderer
int flags = VIRGL_RENDERER_USE_EGL |
            VIRGL_RENDERER_USE_EXTERNAL_BLOB |
            VIRGL_RENDERER_USE_GLES;
virgl_renderer_init(cookie, flags, &cbs);

// 此时:
// - virglrenderer 内部初始化完成
// - 还没有创建任何上下文 (GL 或 virgl_context)
```

### 5.2 上下文创建流程

```cpp
// 4. 创建 virgl_context
int ret = virgl_renderer_context_create_with_flags(
    1,                           // ctx_id
    VIRTGPU_DRM_CAPSET_VIRGL2,  // capset_id
    strlen("vtest"),
    "vtest"
);

// 内部流程:
// ┌─ virgl_renderer_context_create_with_flags() ─┐
// │                                               │
// │  1. 根据 capset_id 选择实现                   │
// │     ctx = vrend_create_context(1, VIRGL2)    │
// │                                               │
// │  2. vrend_create_context 需要 GL 上下文       │
// │     调用 create_gl_context_callback()         │
// │        │                                      │
// │        ├─> 应用创建 EGLContext                │
// │        ├─> 应用创建 EGLSurface                │
// │        └─> 返回 virgl_renderer_gl_context    │
// │                                               │
// │  3. 初始化 virgl_context                      │
// │     ctx->ctx_id = 1                          │
// │     ctx->capset_id = VIRGL2                  │
// │     ctx->submit_cmd = vrend_submit_cmd       │
// │                                               │
// │  4. 添加到全局表                              │
// │     virgl_context_add(ctx)                   │
// │                                               │
// └───────────────────────────────────────────────┘

// 此时:
// - virgl_context(ctx_id=1) 已创建
// - EGLContext 已创建并与 virgl_context 关联
// - 两者都处于可用状态
```

### 5.3 命令提交流程

```cpp
// 5. 提交渲染命令
uint32_t commands[] = {
    VIRGL_CCMD_CLEAR,
    // ... 命令参数
};

int ret = virgl_renderer_submit_cmd(commands, 1, sizeof(commands) / 4);

// 内部流程:
// ┌─ virgl_renderer_submit_cmd() ─┐
// │                                │
// │  1. 查找 virgl_context         │
// │     ctx = virgl_context_lookup(1)
// │                                │
// │  2. 调用上下文方法              │
// │     ctx->submit_cmd(ctx, commands, size)
// │        │                       │
// │        ├─ vrend_submit_cmd()   │
// │        │                       │
// │        └─ 需要 GL 上下文时:     │
// │           make_current_callback(stored_gl_context)
// │              │                  │
// │              ├─ 应用激活 EGL 上下文
// │              │  eglMakeCurrent(ctx, surface)
// │              │                  │
// │              └─ 执行 GL 命令    │
// │                 glClear(...)   │
// │                 glDrawArrays(...)
// │                                │
// │  3. 命令执行完成                │
// │     触发 fence callback         │
// │     write_fence_callback(fence_id)
// │                                │
// └────────────────────────────────┘
```

### 5.4 清理流程

```cpp
// 6. 销毁 virgl_context
virgl_renderer_context_destroy(1);

// 内部流程:
// ┌─ virgl_renderer_context_destroy() ─┐
// │                                     │
// │  1. 查找上下文                       │
// │     ctx = virgl_context_lookup(1)   │
// │                                     │
// │  2. 从全局表移除                     │
// │     virgl_context_remove(1)         │
// │                                     │
// │  3. 调用销毁方法                     │
// │     ctx->destroy(ctx)               │
// │        │                            │
// │        └─ vrend_destroy_context()   │
// │           可能调用                   │
// │           destroy_gl_context_callback()
// │              │                       │
// │              └─ 应用销毁 EGL 上下文  │
// │                 eglDestroyContext()  │
// │                 eglDestroySurface()  │
// │                                     │
// └─────────────────────────────────────┘

// 7. 清理 virglrenderer
virgl_renderer_cleanup(cookie);

// 此时所有资源已释放
```

---

## 6. 实际应用场景

### 6.1 场景 1: 单上下文 (最常见)

```cpp
/**
 * 最简单的场景: 1 个 GL 上下文 + 1 个 virgl_context
 */

// 应用层
EGLContext egl_ctx = eglCreateContext(...);

// virglrenderer 层
virgl_renderer_context_create_with_flags(1, VIRGL2, ...);

// 关系图:
//   EGLContext (egl_ctx)
//        │
//        │ 转换为 virgl_renderer_gl_context
//        │
//        ▼
//   virgl_context(ctx_id=1)
```

**使用代码:**
```cpp
// 初始化
VirGLServerContext ctx;
virgl_renderer_init(&ctx, flags, &callbacks);
virgl_renderer_context_create_with_flags(1, VIRGL2, strlen("ctx"), "ctx");

// 渲染
uint32_t cmds[] = { /* ... */ };
virgl_renderer_submit_cmd(cmds, 1, sizeof(cmds) / 4);

// 清理
virgl_renderer_context_destroy(1);
virgl_renderer_cleanup(&ctx);
```

### 6.2 场景 2: 多显示器 (Multi-Scanout)

```cpp
/**
 * 多显示器场景: 多个 GL 上下文 + 1 个 virgl_context
 * 不同的 scanout_idx 对应不同的显示输出
 */

// 应用层 - 为每个显示器创建 GL 上下文
create_gl_context_callback(cookie, scanout_idx=0, param);  // 主显示器
create_gl_context_callback(cookie, scanout_idx=1, param);  // 副显示器

// virglrenderer 层 - 只有 1 个 virgl_context
virgl_renderer_context_create_with_flags(1, VIRGL2, ...);

// 关系图:
//   EGLContext (scanout 0) ─┐
//                            ├─> virgl_context(ctx_id=1)
//   EGLContext (scanout 1) ─┘

// 使用时,virgl_context 根据需要切换不同的 GL 上下文
```

**使用代码:**
```cpp
struct MultiScanoutContext {
    EGLContext egl_contexts[2];   // 2 个显示器
    EGLSurface egl_surfaces[2];
};

static virgl_renderer_gl_context create_gl_context_callback(
        void *cookie, int scanout_idx,
        struct virgl_renderer_gl_ctx_param *param) {

    auto *ctx = static_cast<MultiScanoutContext *>(cookie);

    // 为特定 scanout 创建上下文
    EGLContext egl_ctx = eglCreateContext(...);
    EGLSurface surface = createSurfaceForScanout(scanout_idx);

    ctx->egl_contexts[scanout_idx] = egl_ctx;
    ctx->egl_surfaces[scanout_idx] = surface;

    return reinterpret_cast<virgl_renderer_gl_context>(egl_ctx);
}

static int make_current_callback(void *cookie, int scanout_idx,
                                 virgl_renderer_gl_context ctx) {
    auto *multi_ctx = static_cast<MultiScanoutContext *>(cookie);

    // 使用对应 scanout 的 surface
    EGLContext egl_ctx = reinterpret_cast<EGLContext>(ctx);
    EGLSurface surface = multi_ctx->egl_surfaces[scanout_idx];

    return eglMakeCurrent(display, surface, surface, egl_ctx) ? 0 : -1;
}
```

### 6.3 场景 3: 多客户端

```cpp
/**
 * 多客户端场景: 多个容器/虚拟机连接
 * 可能共享 GL 上下文或使用独立的
 */

// 客户端 A
virgl_renderer_context_create_with_flags(1, VIRGL2, ...);

// 客户端 B
virgl_renderer_context_create_with_flags(2, VIRGL2, ...);

// 选项 1: 共享 GL 上下文
//   EGLContext (shared)
//        │
//        ├─> virgl_context(ctx_id=1)  // 客户端 A
//        │
//        └─> virgl_context(ctx_id=2)  // 客户端 B

// 选项 2: 独立 GL 上下文
//   EGLContext_A ──> virgl_context(ctx_id=1)
//   EGLContext_B ──> virgl_context(ctx_id=2)
```

**使用代码 (共享上下文):**
```cpp
static virgl_renderer_gl_context g_shared_gl_context = nullptr;

static virgl_renderer_gl_context create_gl_context_callback(
        void *cookie, int scanout_idx,
        struct virgl_renderer_gl_ctx_param *param) {

    if (g_shared_gl_context) {
        // 返回已存在的共享上下文
        return g_shared_gl_context;
    }

    // 首次创建
    EGLContext egl_ctx = eglCreateContext(...);
    g_shared_gl_context = reinterpret_cast<virgl_renderer_gl_context>(egl_ctx);

    return g_shared_gl_context;
}

// 多个 virgl_context 共享同一个 GL 上下文
virgl_renderer_context_create_with_flags(1, VIRGL2, ...);  // 客户端 A
virgl_renderer_context_create_with_flags(2, VIRGL2, ...);  // 客户端 B
```

### 6.4 场景 4: 无外部 winsys (内部管理)

```cpp
/**
 * 如果不使用 VIRGL_RENDERER_USE_EGL 标志
 * virglrenderer 会内部创建 GL 上下文
 * 此时不需要 create_gl_context callback
 */

// 不需要实现 callbacks
struct virgl_renderer_callbacks cbs = {
    .version = 1,
    .write_fence = write_fence_callback,
    // create_gl_context = NULL  (不需要)
    // make_current = NULL        (不需要)
};

// 不使用 USE_EGL 标志
int flags = VIRGL_RENDERER_USE_GLX;  // Linux 使用 GLX
virgl_renderer_init(cookie, flags, &cbs);

// virglrenderer 内部会创建和管理 GL 上下文
// 应用层不需要关心 virgl_renderer_gl_context
```

---

## 7. 最佳实践

### 7.1 Context 管理

**DO: 集中管理 EGL 资源**
```cpp
class EGLManager {
public:
    static EGLManager* instance();

    EGLContext createContext();
    EGLSurface createSurface(...);
    void destroyContext(EGLContext ctx);
    void destroySurface(EGLSurface surface);
    bool makeCurrent(EGLContext ctx, EGLSurface surface);
};

// 在 callbacks 中使用
static virgl_renderer_gl_context create_gl_context_callback(...) {
    EGLContext ctx = EGLManager::instance()->createContext();
    return reinterpret_cast<virgl_renderer_gl_context>(ctx);
}
```

**DON'T: 分散管理导致泄漏**
```cpp
// ❌ 错误: 没有统一管理
static virgl_renderer_gl_context create_gl_context_callback(...) {
    EGLContext ctx = eglCreateContext(...);
    // 没有保存 ctx,销毁时无法找到
    return reinterpret_cast<virgl_renderer_gl_context>(ctx);
}
```

### 7.2 Surface 管理

**DO: 在 cookie 中存储 Surface**
```cpp
struct VirGLServerContext {
    EGLContext egl_ctx;
    EGLSurface egl_surface;  // ✅ 存储 surface
};

static virgl_renderer_gl_context create_gl_context_callback(
        void *cookie, ...) {
    auto *ctx = static_cast<VirGLServerContext *>(cookie);

    ctx->egl_ctx = eglCreateContext(...);
    ctx->egl_surface = eglCreatePbufferSurface(...);  // ✅ 创建 surface

    return reinterpret_cast<virgl_renderer_gl_context>(ctx->egl_ctx);
}

static int make_current_callback(void *cookie, int scanout_idx,
                                 virgl_renderer_gl_context ctx) {
    auto *server_ctx = static_cast<VirGLServerContext *>(cookie);
    EGLContext egl_ctx = reinterpret_cast<EGLContext>(ctx);

    // ✅ 使用存储的 surface
    return eglMakeCurrent(display,
                         server_ctx->egl_surface,
                         server_ctx->egl_surface,
                         egl_ctx) ? 0 : -1;
}
```

**DON'T: 没有 Surface 或每次创建新的**
```cpp
// ❌ 错误: 没有 surface
static int make_current_callback(..., virgl_renderer_gl_context ctx) {
    EGLContext egl_ctx = reinterpret_cast<EGLContext>(ctx);
    return eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, egl_ctx);
    // 会失败! GL 上下文需要 surface
}

// ❌ 错误: 每次创建新 surface (泄漏)
static int make_current_callback(..., virgl_renderer_gl_context ctx) {
    EGLContext egl_ctx = reinterpret_cast<EGLContext>(ctx);
    EGLSurface surface = eglCreatePbufferSurface(...);  // 泄漏!
    return eglMakeCurrent(display, surface, surface, egl_ctx);
}
```

### 7.3 线程安全

**DO: 确保 GL 上下文在正确的线程**
```cpp
// vtest server 在独立线程运行
Thread {
    // ✅ 在此线程初始化并使用 VirGL
    VirGLServer.initVirGL();
    VirGLServer.startServer(clientFd);
}.start();

// GL 命令只在这个线程执行
// EGL 上下文绑定到这个线程
```

**DON'T: 跨线程使用 GL 上下文**
```cpp
// ❌ 错误: 跨线程
Thread A:
    VirGLServer.initVirGL();
    EGLContext ctx = eglGetCurrentContext();  // 绑定到 Thread A

Thread B:
    glDrawArrays(...);  // ❌ 崩溃! 未绑定上下文
```

### 7.4 错误处理

**DO: 检查所有返回值**
```cpp
static virgl_renderer_gl_context create_gl_context_callback(...) {
    EGLContext ctx = eglCreateContext(...);
    if (ctx == EGL_NO_CONTEXT) {
        LOGE("Failed to create EGL context: 0x%x", eglGetError());
        return nullptr;  // ✅ 返回 null 表示失败
    }

    EGLSurface surface = eglCreatePbufferSurface(...);
    if (surface == EGL_NO_SURFACE) {
        LOGE("Failed to create surface: 0x%x", eglGetError());
        eglDestroyContext(display, ctx);  // ✅ 清理已创建的资源
        return nullptr;
    }

    return reinterpret_cast<virgl_renderer_gl_context>(ctx);
}
```

**DON'T: 忽略错误**
```cpp
// ❌ 错误: 不检查返回值
static virgl_renderer_gl_context create_gl_context_callback(...) {
    EGLContext ctx = eglCreateContext(...);  // 可能失败
    return reinterpret_cast<virgl_renderer_gl_context>(ctx);  // 返回 null
}
```

### 7.5 资源清理

**DO: 按相反顺序清理**
```cpp
// 创建顺序
virgl_renderer_init();            // 1
create_gl_context_callback();     // 2
virgl_context_create();           // 3

// 清理顺序 (相反)
virgl_context_destroy();          // 3
destroy_gl_context_callback();    // 2
virgl_renderer_cleanup();         // 1
```

**DON'T: 清理顺序错误**
```cpp
// ❌ 错误: 顺序错误
virgl_renderer_cleanup();         // 先清理 virglrenderer
destroy_gl_context_callback();    // 然后清理 GL (可能已经被使用)
```

---

## 8. 调试技巧

### 8.1 跟踪上下文创建

```cpp
static virgl_renderer_gl_context create_gl_context_callback(
        void *cookie, int scanout_idx,
        struct virgl_renderer_gl_ctx_param *param) {

    LOGD("CREATE_GL_CONTEXT:");
    LOGD("  scanout_idx: %d", scanout_idx);
    LOGD("  major_ver: %d", param->major_ver);
    LOGD("  minor_ver: %d", param->minor_ver);
    LOGD("  shared: %s", param->shared ? "true" : "false");

    EGLContext ctx = eglCreateContext(...);

    LOGD("  created EGLContext: %p", ctx);

    return reinterpret_cast<virgl_renderer_gl_context>(ctx);
}
```

### 8.2 验证上下文状态

```cpp
static int make_current_callback(..., virgl_renderer_gl_context ctx) {
    LOGD("MAKE_CURRENT:");
    LOGD("  requested ctx: %p", ctx);

    EGLContext current_before = eglGetCurrentContext();
    LOGD("  current ctx before: %p", current_before);

    EGLContext egl_ctx = reinterpret_cast<EGLContext>(ctx);
    bool success = eglMakeCurrent(display, surface, surface, egl_ctx);

    if (!success) {
        EGLint error = eglGetError();
        LOGE("  make_current failed: 0x%x", error);
        return -1;
    }

    EGLContext current_after = eglGetCurrentContext();
    LOGD("  current ctx after: %p", current_after);
    LOGD("  success: %s", (current_after == egl_ctx) ? "true" : "false");

    return 0;
}
```

### 8.3 监控 virgl_context 调用

```cpp
int virgl_renderer_submit_cmd(void *buffer, int ctx_id, int ndw) {
    LOGD("SUBMIT_CMD:");
    LOGD("  ctx_id: %u", ctx_id);
    LOGD("  ndw: %d", ndw);

    struct virgl_context *ctx = virgl_context_lookup(ctx_id);
    if (!ctx) {
        LOGE("  context not found!");
        return -EINVAL;
    }

    LOGD("  found virgl_context: %p", ctx);
    LOGD("  capset_id: %u", ctx->capset_id);

    int ret = ctx->submit_cmd(ctx, buffer, ndw * 4);
    LOGD("  submit result: %d", ret);

    return ret;
}
```

---

## 9. 常见问题

### Q1: 为什么需要两层抽象？

**A**: 分离关注点
- **virgl_renderer_gl_context**: 处理平台特定的 GL 上下文 (EGL, GLX, WGL)
- **virgl_context**: 处理虚拟化逻辑，与平台无关

### Q2: 可以不实现 create_gl_context callback 吗？

**A**: 可以，如果:
1. 不使用 `VIRGL_RENDERER_USE_EGL` 标志
2. 让 virglrenderer 内部管理 GL 上下文

但在 Android 上推荐实现 callbacks 以更好地控制 EGL。

### Q3: 一个 virgl_context 可以使用多个 GL 上下文吗？

**A**: 可以，通过 `scanout_idx` 区分:
```cpp
// 创建多个 GL 上下文
create_gl_context(cookie, scanout_idx=0, ...);
create_gl_context(cookie, scanout_idx=1, ...);

// 同一个 virgl_context 根据需要切换
make_current(cookie, scanout_idx=0, ctx_0);  // 使用 ctx_0
make_current(cookie, scanout_idx=1, ctx_1);  // 切换到 ctx_1
```

### Q4: virgl_context 和线程的关系？

**A**: virgl_context 是线程无关的逻辑结构，但:
- 它使用的 GL 上下文 (virgl_renderer_gl_context) 绑定到线程
- 确保在正确的线程调用 virglrenderer API

### Q5: 如何共享 GL 资源？

**A**: 在创建 GL 上下文时使用 `param->shared`:
```cpp
static virgl_renderer_gl_context create_gl_context_callback(..., param) {
    EGLContext shared_ctx = param->shared ? eglGetCurrentContext() : EGL_NO_CONTEXT;
    EGLContext ctx = eglCreateContext(display, config, shared_ctx, ...);
    return reinterpret_cast<virgl_renderer_gl_context>(ctx);
}
```

---

## 10. 参考资源

### 头文件
- `virglrenderer/src/virglrenderer.h` - virgl_renderer_gl_context 定义
- `virglrenderer/src/virgl_context.h` - virgl_context 定义
- `gpu/src/main/cpp/virgl_jni.cpp` - 项目中的实现示例

### 文档
- `gpu/VIRGL_VTEST_SERVER_GUIDE.md` - vtest server 实现教程
- `gpu/VIRGL_API_REFERENCE.md` - API 参考手册

### 官方资源
- https://gitlab.freedesktop.org/virgl/virglrenderer
- https://www.freedesktop.org/wiki/Software/virglrenderer/

---

## 总结

**核心要点:**

1. **virgl_renderer_gl_context** 是外部层，代表真实的 EGL/OpenGL 上下文
2. **virgl_context** 是内部层，代表 VirGL 的逻辑渲染上下文
3. **关系**: virgl_context 通过 callbacks 使用 virgl_renderer_gl_context
4. **管理**: 应用管理 GL 上下文，virglrenderer 管理逻辑上下文
5. **生命周期**: GL 上下文可能跨越多个 virgl_context

**类比:**
- **virgl_renderer_gl_context** = 厨师 (实际工作者)
- **virgl_context** = 订单 (工作任务)
- 一个订单可能需要多个厨师协作，或多个订单共用一个厨师
