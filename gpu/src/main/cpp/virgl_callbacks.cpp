#include "virgl_callbacks.h"
#include "virgl_egl_manager.h"
#include <android/log.h>
#include <errno.h>

#define LOG_TAG "VirGL-Callbacks"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

void* get_egl_display_callback(void* cookie) {
    EGLDisplay display = EGLManager::instance()->getDisplay();
    LOGD("get_egl_display_callback() -> %p", display);
    return reinterpret_cast<void*>(display);
}

virgl_renderer_gl_context create_gl_context_callback(
        void* cookie,
        int scanout_idx,
        struct virgl_renderer_gl_ctx_param* param) {

    LOGD("create_gl_context_callback(scanout_idx=%d, shared=%d, major=%d, minor=%d)",
         scanout_idx, param->shared, param->major_ver, param->minor_ver);

    auto* server_ctx = static_cast<ServerContext*>(cookie);

    // Determine shared context
    EGLContext shared = EGL_NO_CONTEXT;
    if (param->shared && server_ctx->shared_context != EGL_NO_CONTEXT) {
        shared = server_ctx->shared_context;
    }

    // Create EGL context
    EGLContext egl_ctx = EGLManager::instance()->createContext(shared);
    if (egl_ctx == EGL_NO_CONTEXT) {
        LOGE("Failed to create EGL context");
        return nullptr;
    }

    LOGD("Created virgl GL context: %p (shared with %p)", egl_ctx, shared);

    // Return as opaque pointer
    return reinterpret_cast<virgl_renderer_gl_context>(egl_ctx);
}

void destroy_gl_context_callback(void* cookie, virgl_renderer_gl_context ctx) {
    if (!ctx) {
        return;
    }

    EGLContext egl_ctx = reinterpret_cast<EGLContext>(ctx);
    LOGD("destroy_gl_context_callback(%p)", egl_ctx);

    EGLManager::instance()->destroyContext(egl_ctx);
}

int make_current_callback(void* cookie, int scanout_idx, virgl_renderer_gl_context ctx) {
    auto* server_ctx = static_cast<ServerContext*>(cookie);

    // Special case: unbind context
    if (ctx == nullptr) {
        LOGD("make_current_callback(nullptr) - unbinding");
        bool success = EGLManager::instance()->makeCurrent(EGL_NO_CONTEXT, EGL_NO_SURFACE);
        return success ? 0 : -EINVAL;
    }

    // Make context current with shared base surface
    EGLContext egl_ctx = reinterpret_cast<EGLContext>(ctx);
    bool success = EGLManager::instance()->makeCurrent(egl_ctx, server_ctx->shared_surface);

    if (!success) {
        LOGE("make_current_callback(%p) failed", egl_ctx);
        return -EINVAL;
    }

    return 0;
}

void write_fence_callback(void* cookie, uint32_t fence_id) {
    LOGD("write_fence_callback(fence_id=%u) - rendering complete", fence_id);

    // Future: Can use eventfd or pipe to notify clients about fence completion
    // For now, just log it
}
