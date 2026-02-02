/**
 * VirGL Context
 *
 * Manages per-renderer state including EGL context and resources
 */

#ifndef VIRGL_CONTEXT_H
#define VIRGL_CONTEXT_H

#include <EGL/egl.h>
#include <jni.h>
#include <android/native_window.h>
#include <unordered_map>
#include <memory>
#include <mutex>

/**
 * Resource information
 */
struct ResourceInfo {
    uint32_t res_id;
    uint32_t blob_mem;  // GUEST, HOST3D, etc.
    uint64_t size;
    int exported_fd;    // -1 if not exported
};

/**
 * Sync object information
 */
struct SyncInfo {
    uint32_t sync_id;
    uint64_t current_value;
    int eventfd;  // -1 if not using eventfd
};

/**
 * VirGL rendering context (one per client)
 */
struct VirGLContext {
    uint64_t renderer_id;
    int ctx_id;  // VirGL context ID

    // EGL state
    EGLContext egl_ctx = EGL_NO_CONTEXT;
    EGLSurface egl_surface = EGL_NO_SURFACE;
    ANativeWindow* native_window = nullptr;

    // Java references
    jobject surface_ref = nullptr;     // Global ref to Surface
    jobject renderer_ref = nullptr;    // Global ref to VirGLRenderer (for callbacks)

    // Resource tracking
    std::unordered_map<uint32_t, ResourceInfo> resources;
    std::mutex resources_mutex;

    // Sync tracking
    std::unordered_map<uint32_t, SyncInfo> syncs;
    std::mutex syncs_mutex;
    uint32_t next_sync_id = 1;

    // Thread info
    pthread_t thread_id = 0;

    VirGLContext(uint64_t id, int ctx)
        : renderer_id(id), ctx_id(ctx) {}

    ~VirGLContext();
};

// Global context map
extern std::unordered_map<uint64_t, std::unique_ptr<VirGLContext>> g_contexts;
extern std::mutex g_contexts_mutex;

// Helper to get context
VirGLContext* getContext(uint64_t renderer_id);

#endif // VIRGL_CONTEXT_H
