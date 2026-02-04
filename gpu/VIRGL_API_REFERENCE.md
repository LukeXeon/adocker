# VirGL API 参考手册

## virglrenderer 核心 API

### 初始化与清理

#### virgl_renderer_init
```c
int virgl_renderer_init(void *cookie, int flags,
                       struct virgl_renderer_callbacks *cbs);
```
**功能**: 初始化 virglrenderer

**参数**:
- `cookie`: 用户数据指针，会传递给所有回调函数
- `flags`: 初始化标志
- `cbs`: 回调函数结构

**标志位**:
```c
#define VIRGL_RENDERER_USE_EGL              (1 << 0)  // 使用 EGL
#define VIRGL_RENDERER_THREAD_SYNC          (1 << 1)  // 线程同步
#define VIRGL_RENDERER_USE_GLX              (1 << 2)  // 使用 GLX (Linux)
#define VIRGL_RENDERER_USE_SURFACELESS      (1 << 3)  // 无 Surface
#define VIRGL_RENDERER_USE_GLES             (1 << 4)  // 使用 GLES
#define VIRGL_RENDERER_USE_EXTERNAL_BLOB    (1 << 5)  // 外部 Blob
#define VIRGL_RENDERER_VENUS                (1 << 6)  // Venus 渲染器
#define VIRGL_RENDERER_NO_VIRGL             (1 << 7)  // 禁用 VirGL
#define VIRGL_RENDERER_ASYNC_FENCE_CB       (1 << 8)  // 异步 Fence
#define VIRGL_RENDERER_RENDER_SERVER        (1 << 9)  // 渲染服务器
#define VIRGL_RENDERER_DRM                  (1 << 10) // DRM 渲染器
```

**Android 推荐配置**:
```c
int flags = VIRGL_RENDERER_USE_EGL |
            VIRGL_RENDERER_USE_EXTERNAL_BLOB |
            VIRGL_RENDERER_USE_GLES;
```

**返回值**: 0 成功，负数失败

---

#### virgl_renderer_cleanup
```c
void virgl_renderer_cleanup(void *cookie);
```
**功能**: 清理 virglrenderer，释放所有资源

---

### 上下文管理

#### virgl_renderer_context_create
```c
int virgl_renderer_context_create(uint32_t handle, uint32_t nlen,
                                  const char *name);
```
**功能**: 创建渲染上下文 (旧版)

**参数**:
- `handle`: 上下文 ID
- `nlen`: 名称长度
- `name`: 上下文名称

---

#### virgl_renderer_context_create_with_flags
```c
int virgl_renderer_context_create_with_flags(uint32_t ctx_id,
                                             uint32_t ctx_flags,
                                             uint32_t nlen,
                                             const char *name);
```
**功能**: 创建带标志的渲染上下文 (推荐)

**参数**:
- `ctx_id`: 上下文 ID
- `ctx_flags`: 上下文标志 (capset_id)
- `nlen`: 名称长度
- `name`: 上下文名称

**ctx_flags (capset_id)**:
```c
#define VIRTGPU_DRM_CAPSET_VIRGL   1  // VirGL
#define VIRTGPU_DRM_CAPSET_VIRGL2  2  // VirGL2 (推荐)
#define VIRTGPU_DRM_CAPSET_VENUS   3  // Venus (Vulkan)
```

**示例**:
```c
int ret = virgl_renderer_context_create_with_flags(
    1, VIRTGPU_DRM_CAPSET_VIRGL2, strlen("vtest"), "vtest");
```

---

#### virgl_renderer_context_destroy
```c
void virgl_renderer_context_destroy(uint32_t handle);
```
**功能**: 销毁渲染上下文

---

### 资源管理

#### virgl_renderer_resource_create
```c
int virgl_renderer_resource_create(
    struct virgl_renderer_resource_create_args *args,
    struct iovec *iov,
    uint32_t num_iovs);
```
**功能**: 创建资源 (纹理、缓冲区等)

**参数结构**:
```c
struct virgl_renderer_resource_create_args {
    uint32_t handle;      // 资源 ID
    uint32_t target;      // 目标类型 (PIPE_TEXTURE_2D, etc.)
    uint32_t format;      // 格式 (PIPE_FORMAT_*)
    uint32_t bind;        // 绑定标志
    uint32_t width;       // 宽度
    uint32_t height;      // 高度
    uint32_t depth;       // 深度
    uint32_t array_size;  // 数组大小
    uint32_t last_level;  // Mipmap 级别
    uint32_t nr_samples;  // 采样数
    uint32_t flags;       // 标志
};
```

**绑定标志**:
```c
#define VIRGL_RES_BIND_DEPTH_STENCIL   (1 << 0)
#define VIRGL_RES_BIND_RENDER_TARGET   (1 << 1)
#define VIRGL_RES_BIND_SAMPLER_VIEW    (1 << 3)
#define VIRGL_RES_BIND_VERTEX_BUFFER   (1 << 4)
#define VIRGL_RES_BIND_INDEX_BUFFER    (1 << 5)
#define VIRGL_RES_BIND_CONSTANT_BUFFER (1 << 6)
#define VIRGL_RES_BIND_STREAM_OUTPUT   (1 << 11)
#define VIRGL_RES_BIND_CURSOR          (1 << 16)
#define VIRGL_RES_BIND_CUSTOM          (1 << 17)
#define VIRGL_RES_BIND_SCANOUT         (1 << 18)
#define VIRGL_RES_BIND_SHARED          (1 << 20)
```

---

#### virgl_renderer_resource_create_blob
```c
int virgl_renderer_resource_create_blob(
    const struct virgl_renderer_resource_create_blob_args *args);
```
**功能**: 创建 Blob 资源 (内存块)

**参数结构**:
```c
struct virgl_renderer_resource_create_blob_args {
    uint32_t res_handle;   // 资源 ID (输入 0，输出实际 ID)
    uint32_t ctx_id;       // 上下文 ID
    uint32_t blob_mem;     // Blob 类型
    uint32_t blob_flags;   // Blob 标志
    uint64_t blob_id;      // Blob ID (唯一标识)
    uint64_t size;         // 大小
    const struct iovec *iovecs; // I/O 向量
    uint32_t num_iovs;     // I/O 向量数量
};
```

**Blob 类型**:
```c
#define VIRGL_RENDERER_BLOB_MEM_GUEST        0x0001  // 客户端内存
#define VIRGL_RENDERER_BLOB_MEM_HOST3D       0x0002  // 主机 3D 内存
#define VIRGL_RENDERER_BLOB_MEM_HOST3D_GUEST 0x0003  // 共享内存
#define VIRGL_RENDERER_BLOB_MEM_GUEST_VRAM   0x0004  // 客户端 VRAM
```

**Blob 标志**:
```c
#define VIRGL_RENDERER_BLOB_FLAG_USE_MAPPABLE     0x0001  // 可映射
#define VIRGL_RENDERER_BLOB_FLAG_USE_SHAREABLE    0x0002  // 可共享
#define VIRGL_RENDERER_BLOB_FLAG_USE_CROSS_DEVICE 0x0004  // 跨设备
```

**示例**:
```c
struct virgl_renderer_resource_create_blob_args args = {};
args.res_handle = 0;  // virgl 分配
args.ctx_id = 1;
args.blob_mem = VIRGL_RENDERER_BLOB_MEM_HOST3D;
args.blob_flags = VIRGL_RENDERER_BLOB_FLAG_USE_MAPPABLE;
args.blob_id = 12345;
args.size = 1024 * 1024;  // 1MB

int ret = virgl_renderer_resource_create_blob(&args);
// args.res_handle 现在包含分配的资源 ID
```

---

#### virgl_renderer_resource_unref
```c
void virgl_renderer_resource_unref(uint32_t res_handle);
```
**功能**: 释放资源引用

---

#### virgl_renderer_resource_attach_iov
```c
int virgl_renderer_resource_attach_iov(int res_handle,
                                       struct iovec *iov,
                                       int num_iovs);
```
**功能**: 附加 I/O 向量到资源

---

#### virgl_renderer_resource_detach_iov
```c
void virgl_renderer_resource_detach_iov(int res_handle,
                                        struct iovec **iov,
                                        int *num_iovs);
```
**功能**: 分离资源的 I/O 向量

---

### 数据传输

#### virgl_renderer_transfer_write_iov
```c
int virgl_renderer_transfer_write_iov(uint32_t handle,
                                      uint32_t ctx_id,
                                      int level,
                                      uint32_t stride,
                                      uint32_t layer_stride,
                                      struct virgl_box *box,
                                      uint64_t offset,
                                      struct iovec *iovec,
                                      unsigned int iovec_cnt);
```
**功能**: 写入数据到资源 (上传)

**参数**:
- `handle`: 资源 ID
- `ctx_id`: 上下文 ID
- `level`: Mipmap 级别
- `stride`: 行跨度
- `layer_stride`: 层跨度
- `box`: 区域 (x, y, z, width, height, depth)
- `offset`: 偏移量
- `iovec`: I/O 向量
- `iovec_cnt`: I/O 向量数量

**virgl_box 结构**:
```c
struct virgl_box {
    uint32_t x, y, z;
    uint32_t width, height, depth;
};
```

**示例**:
```c
uint8_t data[1024];
struct iovec iov = { data, sizeof(data) };
struct virgl_box box = { 0, 0, 0, 256, 256, 1 };

int ret = virgl_renderer_transfer_write_iov(
    res_id, ctx_id, 0, 256 * 4, 0, &box, 0, &iov, 1);
```

---

#### virgl_renderer_transfer_read_iov
```c
int virgl_renderer_transfer_read_iov(uint32_t handle,
                                     uint32_t ctx_id,
                                     uint32_t level,
                                     uint32_t stride,
                                     uint32_t layer_stride,
                                     struct virgl_box *box,
                                     uint64_t offset,
                                     struct iovec *iov,
                                     int iovec_cnt);
```
**功能**: 从资源读取数据 (下载)

---

### 命令提交

#### virgl_renderer_submit_cmd
```c
int virgl_renderer_submit_cmd(void *buffer,
                              int ctx_id,
                              int ndw);
```
**功能**: 提交渲染命令

**参数**:
- `buffer`: 命令缓冲区 (uint32_t 数组)
- `ctx_id`: 上下文 ID
- `ndw`: 命令数量 (dword)

**重要**:
- buffer 必须至少 4 字节对齐
- buffer 内容是 VirGL 命令流
- 此函数不会修改 buffer

**示例**:
```c
uint32_t cmds[] = {
    VIRGL_CCMD_CLEAR,
    // ... 命令参数
};

int ret = virgl_renderer_submit_cmd(cmds, ctx_id, sizeof(cmds) / 4);
```

---

### Fence 管理

#### virgl_renderer_create_fence
```c
int virgl_renderer_create_fence(int client_fence_id, uint32_t ctx_id);
```
**功能**: 创建 Fence (旧版，用于 ctx0)

---

#### virgl_renderer_context_create_fence
```c
int virgl_renderer_context_create_fence(uint32_t ctx_id,
                                        uint32_t flags,
                                        uint32_t ring_idx,
                                        uint64_t fence_id);
```
**功能**: 创建上下文 Fence (推荐)

**标志**:
```c
#define VIRGL_RENDERER_FENCE_FLAG_MERGEABLE (1 << 0)
```

---

#### virgl_renderer_poll
```c
void virgl_renderer_poll(void);
```
**功能**: 强制处理待定的 Fence

---

#### virgl_renderer_context_poll
```c
void virgl_renderer_context_poll(uint32_t ctx_id);
```
**功能**: 强制处理特定上下文的 Fence

---

### Capabilities

#### virgl_renderer_get_cap_set
```c
void virgl_renderer_get_cap_set(uint32_t set, uint32_t *max_ver,
                                uint32_t *max_size);
```
**功能**: 获取能力集版本和大小

**参数**:
- `set`: 能力集 ID (VIRTGPU_DRM_CAPSET_*)
- `max_ver`: 输出最大版本号
- `max_size`: 输出数据大小 (字节)

---

#### virgl_renderer_fill_caps
```c
void virgl_renderer_fill_caps(uint32_t set, uint32_t version, void *caps);
```
**功能**: 填充能力数据

**示例**:
```c
uint32_t max_ver, max_size;
virgl_renderer_get_cap_set(VIRTGPU_DRM_CAPSET_VIRGL2, &max_ver, &max_size);

if (max_size > 0) {
    void *caps = malloc(max_size);
    virgl_renderer_fill_caps(VIRTGPU_DRM_CAPSET_VIRGL2, max_ver, caps);
    // 使用 caps 数据
    free(caps);
}
```

---

### 资源导出

#### virgl_renderer_resource_export_blob
```c
int virgl_renderer_resource_export_blob(uint32_t res_id, uint32_t *fd_type,
                                        int *fd);
```
**功能**: 导出 Blob 资源为文件描述符

**参数**:
- `res_id`: 资源 ID
- `fd_type`: 输出文件描述符类型
- `fd`: 输出文件描述符

**FD 类型**:
```c
#define VIRGL_RENDERER_BLOB_FD_TYPE_DMABUF  0x0001  // DMA-BUF
#define VIRGL_RENDERER_BLOB_FD_TYPE_OPAQUE  0x0002  // 不透明
#define VIRGL_RENDERER_BLOB_FD_TYPE_SHM     0x0003  // 共享内存
```

**示例**:
```c
uint32_t fd_type;
int fd;
int ret = virgl_renderer_resource_export_blob(res_id, &fd_type, &fd);
if (ret == 0) {
    // 使用 fd
    close(fd);
}
```

---

#### virgl_renderer_resource_map
```c
int virgl_renderer_resource_map(uint32_t res_handle, void **map,
                                uint64_t *out_size);
```
**功能**: 映射资源到内存

---

#### virgl_renderer_resource_unmap
```c
int virgl_renderer_resource_unmap(uint32_t res_handle);
```
**功能**: 取消资源映射

---

### 资源信息

#### virgl_renderer_resource_get_info
```c
int virgl_renderer_resource_get_info(int res_handle,
                                     struct virgl_renderer_resource_info *info);
```
**功能**: 获取资源信息

**信息结构**:
```c
struct virgl_renderer_resource_info {
    uint32_t handle;       // 资源 ID
    uint32_t virgl_format; // VirGL 格式
    uint32_t width;        // 宽度
    uint32_t height;       // 高度
    uint32_t depth;        // 深度
    uint32_t flags;        // 标志
    uint32_t tex_id;       // OpenGL 纹理 ID
    uint32_t stride;       // 跨度
    int drm_fourcc;        // DRM FourCC
    int fd;                // 文件描述符
};
```

---

## Callbacks 详解

### virgl_renderer_callbacks 结构

```c
struct virgl_renderer_callbacks {
    int version;  // 版本号 (最新: 4)

    // v1: 基础回调
    void (*write_fence)(void *cookie, uint32_t fence);

    // v1: GL 上下文管理 (使用外部 winsys 时需要)
    virgl_renderer_gl_context (*create_gl_context)(
        void *cookie, int scanout_idx,
        struct virgl_renderer_gl_ctx_param *param);
    void (*destroy_gl_context)(void *cookie,
        virgl_renderer_gl_context ctx);
    int (*make_current)(void *cookie, int scanout_idx,
        virgl_renderer_gl_context ctx);

    // v2: DRM 支持 (可选)
    int (*get_drm_fd)(void *cookie);

    // v3: 多上下文 Fence (可选)
    void (*write_context_fence)(void *cookie, uint32_t ctx_id,
        uint32_t ring_idx, uint64_t fence_id);

    // v3: 外部渲染服务器 (可选)
    int (*get_server_fd)(void *cookie, uint32_t version);

    // v4: 外部 EGLDisplay (Android 推荐)
    void *(*get_egl_display)(void *cookie);
};
```

### create_gl_context 参数

```c
struct virgl_renderer_gl_ctx_param {
    int version;      // 版本 (2)
    bool shared;      // 是否共享上下文
    int major_ver;    // GL 主版本号 (3)
    int minor_ver;    // GL 次版本号 (0)
    int compat_ctx;   // 兼容上下文标志
};
```

---

## 错误码

```c
// 成功
#define 0  // 成功

// 常见错误
#define -EINVAL   // 无效参数
#define -ENOMEM   // 内存不足
#define -ENOENT   // 未找到
#define -EBUSY    // 资源忙
#define -EAGAIN   // 重试
#define -ETIMEDOUT // 超时
```

---

## 常用宏定义

### Pipe 格式 (部分)

```c
#define PIPE_FORMAT_B8G8R8A8_UNORM  1   // BGRA8888
#define PIPE_FORMAT_B8G8R8X8_UNORM  2   // BGRX8888
#define PIPE_FORMAT_R8G8B8A8_UNORM  67  // RGBA8888
#define PIPE_FORMAT_R8G8B8X8_UNORM  68  // RGBX8888
```

### Pipe 纹理目标

```c
#define PIPE_BUFFER        0  // 缓冲区
#define PIPE_TEXTURE_1D    1  // 1D 纹理
#define PIPE_TEXTURE_2D    2  // 2D 纹理
#define PIPE_TEXTURE_3D    3  // 3D 纹理
#define PIPE_TEXTURE_CUBE  4  // 立方体贴图
#define PIPE_TEXTURE_RECT  5  // 矩形纹理
```

---

## 使用模式

### 基本初始化流程

```c
// 1. 初始化 virglrenderer
struct virgl_renderer_callbacks cbs = { /* ... */ };
virgl_renderer_init(cookie, flags, &cbs);

// 2. 创建上下文
virgl_renderer_context_create_with_flags(1, VIRTGPU_DRM_CAPSET_VIRGL2,
                                         strlen("ctx"), "ctx");

// 3. 创建资源
struct virgl_renderer_resource_create_blob_args args = { /* ... */ };
virgl_renderer_resource_create_blob(&args);

// 4. 提交命令
uint32_t cmds[] = { /* ... */ };
virgl_renderer_submit_cmd(cmds, 1, sizeof(cmds) / 4);

// 5. 创建 Fence
virgl_renderer_context_create_fence(1, 0, 0, fence_id);

// 6. 清理
virgl_renderer_resource_unref(res_id);
virgl_renderer_context_destroy(1);
virgl_renderer_cleanup(cookie);
```

---

## 参考资源

- **头文件**: `virglrenderer/src/virglrenderer.h`
- **Gallium 定义**: `virglrenderer/src/gallium/include/pipe/`
- **VirGL 协议**: `virglrenderer/src/virgl_protocol.h`
- **官方文档**: https://gitlab.freedesktop.org/virgl/virglrenderer
