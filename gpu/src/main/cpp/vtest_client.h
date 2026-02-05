#ifndef VTEST_CLIENT_H
#define VTEST_CLIENT_H

#include "vtest_io.h"
#include "vtest_protocol.h"
#include "virgl_callbacks.h"
#include <EGL/egl.h>
#include <android/native_window.h>
#include <unordered_map>
#include <mutex>

/**
 * vtest Client Handler
 *
 * Manages per-client state:
 * - vtest protocol parsing and command dispatch
 * - virgl context lifecycle
 * - EGL context and surface management
 * - Resource tracking
 */
class VTestClient {
public:
    VTestClient(int fd, ServerContext* server_ctx);
    ~VTestClient();

    /**
     * Process commands from client (edge-triggered)
     * @return 0 on success, -EAGAIN if no data, -errno on error
     */
    int processCommands();

    /**
     * Set EGL surface (for SurfaceView rendering)
     * @param surface EGL surface
     * @param window Native window (optional, will be acquired)
     */
    void setEGLSurface(EGLSurface surface, ANativeWindow* window);

private:
    // Command handlers
    int handleProtocolVersion(const VTestHeader& header);
    int handleGetCaps2(const VTestHeader& header);
    int handleGetCapset(const VTestHeader& header);
    int handleContextInit(const VTestHeader& header);
    int handleResourceCreateBlob(const VTestHeader& header);
    int handleResourceUnref(const VTestHeader& header);
    int handleSubmitCmd(const VTestHeader& header);
    int handleTransferPut2(const VTestHeader& header);
    int handleTransferGet2(const VTestHeader& header);

    // Dispatch
    int dispatchCommand(const VTestHeader& header);

    int fd_;
    ServerContext* server_ctx_;
    VTestIO io_;

    uint32_t virgl_ctx_id_ = 0;
    EGLContext egl_ctx_ = EGL_NO_CONTEXT;
    EGLSurface egl_surface_ = EGL_NO_SURFACE;
    ANativeWindow* native_window_ = nullptr;

    std::mutex surface_mutex_;
    std::unordered_map<uint32_t, uint32_t> resources_;  // res_id -> handle

    static uint32_t next_ctx_id_;
};

#endif // VTEST_CLIENT_H
