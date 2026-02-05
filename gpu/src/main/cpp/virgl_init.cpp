#include "virgl_init.h"
#include "virgl_egl_manager.h"
#include <android/log.h>

#define LOG_TAG "VirGL-Init"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

bool initializeVirGL(ServerContext* server_ctx) {
    LOGI("Initializing VirGL renderer");

    if (!server_ctx) {
        LOGE("Invalid server context");
        return false;
    }

    // 1. Initialize EGL Manager
    if (!EGLManager::instance()->initialize()) {
        LOGE("Failed to initialize EGL Manager");
        return false;
    }

    // 2. Create shared base context (all client contexts will share with this)
    server_ctx->shared_context = EGLManager::instance()->createContext(EGL_NO_CONTEXT);
    if (server_ctx->shared_context == EGL_NO_CONTEXT) {
        LOGE("Failed to create shared base EGL context");
        return false;
    }

    LOGI("Created shared base context: %p", server_ctx->shared_context);

    // 3. Create a small pbuffer surface for the shared context
    // (EGL requires a surface to make context current)
    server_ctx->shared_surface = EGLManager::instance()->createPbufferSurface(1, 1);
    if (server_ctx->shared_surface == EGL_NO_SURFACE) {
        LOGE("Failed to create shared base surface");
        EGLManager::instance()->destroyContext(server_ctx->shared_context);
        server_ctx->shared_context = EGL_NO_CONTEXT;
        return false;
    }

    LOGI("Created shared base surface: %p", server_ctx->shared_surface);

    // 4. Set up virglrenderer callbacks
    struct virgl_renderer_callbacks callbacks = {};
    callbacks.version = 4;  // Latest callback version
    callbacks.get_egl_display = get_egl_display_callback;
    callbacks.create_gl_context = create_gl_context_callback;
    callbacks.destroy_gl_context = destroy_gl_context_callback;
    callbacks.make_current = make_current_callback;
    callbacks.write_fence = write_fence_callback;

    // 5. Initialize virglrenderer
    // Flags:
    // - VIRGL_RENDERER_USE_EGL: Use EGL (required on Android)
    // - VIRGL_RENDERER_USE_EXTERNAL_BLOB: Support external blob resources
    // - VIRGL_RENDERER_USE_GLES: Use OpenGL ES instead of desktop GL
    int flags = VIRGL_RENDERER_USE_EGL |
                VIRGL_RENDERER_USE_EXTERNAL_BLOB |
                VIRGL_RENDERER_USE_GLES;

    int ret = virgl_renderer_init(server_ctx, flags, &callbacks);
    if (ret != 0) {
        LOGE("virgl_renderer_init() failed: %d", ret);

        // Cleanup on failure
        EGLManager::instance()->destroySurface(server_ctx->shared_surface);
        EGLManager::instance()->destroyContext(server_ctx->shared_context);
        server_ctx->shared_surface = EGL_NO_SURFACE;
        server_ctx->shared_context = EGL_NO_CONTEXT;

        return false;
    }

    LOGI("VirGL renderer initialized successfully");
    LOGI("  Flags: EGL=%d, ExternalBlob=%d, GLES=%d",
         (flags & VIRGL_RENDERER_USE_EGL) != 0,
         (flags & VIRGL_RENDERER_USE_EXTERNAL_BLOB) != 0,
         (flags & VIRGL_RENDERER_USE_GLES) != 0);

    return true;
}

void cleanupVirGL(ServerContext* server_ctx) {
    LOGI("Cleaning up VirGL renderer");

    if (!server_ctx) {
        return;
    }

    // 1. Cleanup virglrenderer
    virgl_renderer_cleanup(server_ctx);

    // 2. Destroy shared base surface and context
    if (server_ctx->shared_surface != EGL_NO_SURFACE) {
        EGLManager::instance()->destroySurface(server_ctx->shared_surface);
        server_ctx->shared_surface = EGL_NO_SURFACE;
    }

    if (server_ctx->shared_context != EGL_NO_CONTEXT) {
        EGLManager::instance()->destroyContext(server_ctx->shared_context);
        server_ctx->shared_context = EGL_NO_CONTEXT;
    }

    LOGI("VirGL cleanup complete");
}
