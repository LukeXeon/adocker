#ifndef VIRGL_CALLBACKS_H
#define VIRGL_CALLBACKS_H

extern "C" {
#include "virglrenderer/src/virglrenderer.h"
}

#include <EGL/egl.h>

/**
 * Server Context - Cookie passed to all virglrenderer callbacks
 *
 * This structure holds the shared base EGL context that all client
 * contexts will share resources with.
 */
struct ServerContext {
    EGLContext shared_context = EGL_NO_CONTEXT;
    EGLSurface shared_surface = EGL_NO_SURFACE;
};

/**
 * VirGL Callback: Get EGL Display
 *
 * Called by virglrenderer to obtain the EGLDisplay.
 * This is required when using VIRGL_RENDERER_USE_EGL flag.
 *
 * @param cookie ServerContext pointer
 * @return EGLDisplay cast to void*
 */
void* get_egl_display_callback(void* cookie);

/**
 * VirGL Callback: Create GL Context
 *
 * Called by virglrenderer when it needs a new GL context.
 * We create an EGL context that shares resources with the base context.
 *
 * @param cookie ServerContext pointer
 * @param scanout_idx Scanout index (ignored for Android)
 * @param param Context creation parameters
 * @return virgl_renderer_gl_context (EGLContext cast)
 */
virgl_renderer_gl_context create_gl_context_callback(
    void* cookie,
    int scanout_idx,
    struct virgl_renderer_gl_ctx_param* param);

/**
 * VirGL Callback: Destroy GL Context
 *
 * Called by virglrenderer to destroy a GL context.
 *
 * @param cookie ServerContext pointer
 * @param ctx virgl_renderer_gl_context to destroy
 */
void destroy_gl_context_callback(
    void* cookie,
    virgl_renderer_gl_context ctx);

/**
 * VirGL Callback: Make Current
 *
 * Called by virglrenderer to activate a GL context.
 *
 * @param cookie ServerContext pointer
 * @param scanout_idx Scanout index (ignored for Android)
 * @param ctx virgl_renderer_gl_context to make current
 * @return 0 on success, -EINVAL on failure
 */
int make_current_callback(
    void* cookie,
    int scanout_idx,
    virgl_renderer_gl_context ctx);

/**
 * VirGL Callback: Write Fence
 *
 * Called by virglrenderer when rendering is complete (fence signaled).
 * Can be used to notify clients about completion.
 *
 * @param cookie ServerContext pointer
 * @param fence_id Fence ID that completed
 */
void write_fence_callback(void* cookie, uint32_t fence_id);

#endif // VIRGL_CALLBACKS_H
