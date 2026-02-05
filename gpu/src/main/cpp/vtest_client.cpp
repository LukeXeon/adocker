#include "vtest_client.h"
#include "virgl_egl_manager.h"
#include <android/log.h>
#include <unistd.h>
#include <errno.h>
#include <cstring>

extern "C" {
#include "virglrenderer/src/virglrenderer.h"
#include "virglrenderer/src/virgl_protocol.h"
#include "virglrenderer/src/virgl_hw.h"
}

#define LOG_TAG "VTest-Client"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

uint32_t VTestClient::next_ctx_id_ = 1;

VTestClient::VTestClient(int fd, ServerContext* server_ctx)
    : fd_(fd), server_ctx_(server_ctx), io_(fd) {

    LOGD("VTestClient created (fd=%d)", fd);

    // Create client EGL context (shares resources with base context)
    egl_ctx_ = EGLManager::instance()->createContext(server_ctx_->shared_context);
    if (egl_ctx_ == EGL_NO_CONTEXT) {
        LOGE("Failed to create client EGL context");
        return;
    }

    // Create initial pbuffer surface
    egl_surface_ = EGLManager::instance()->createPbufferSurface(1280, 720);
    if (egl_surface_ == EGL_NO_SURFACE) {
        LOGE("Failed to create client EGL surface");
        EGLManager::instance()->destroyContext(egl_ctx_);
        egl_ctx_ = EGL_NO_CONTEXT;
        return;
    }

    LOGD("Client EGL context %p and surface %p created", egl_ctx_, egl_surface_);
}

VTestClient::~VTestClient() {
    LOGD("VTestClient destroyed (fd=%d, ctx_id=%u)", fd_, virgl_ctx_id_);

    // Cleanup resources
    for (auto& pair : resources_) {
        virgl_renderer_resource_unref(pair.second);
    }
    resources_.clear();

    // Destroy virgl context
    if (virgl_ctx_id_ != 0) {
        virgl_renderer_context_destroy(virgl_ctx_id_);
        virgl_ctx_id_ = 0;
    }

    // Destroy EGL surface
    if (egl_surface_ != EGL_NO_SURFACE) {
        EGLManager::instance()->destroySurface(egl_surface_);
        egl_surface_ = EGL_NO_SURFACE;
    }

    // Destroy EGL context
    if (egl_ctx_ != EGL_NO_CONTEXT) {
        EGLManager::instance()->destroyContext(egl_ctx_);
        egl_ctx_ = EGL_NO_CONTEXT;
    }

    // Release native window
    if (native_window_) {
        ANativeWindow_release(native_window_);
        native_window_ = nullptr;
    }

    // Close socket
    if (fd_ >= 0) {
        close(fd_);
    }
}

int VTestClient::processCommands() {
    VTestHeader header;
    int ret = io_.readHeader(header);
    if (ret != 0) {
        return ret;
    }

    return dispatchCommand(header);
}

int VTestClient::dispatchCommand(const VTestHeader& header) {
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
            return handleTransferPut2(header);

        case VCMD_TRANSFER_GET2:
            return handleTransferGet2(header);

        default:
            LOGW("Unhandled command: %u (length=%u)", header.cmd_id, header.length);
            // Read and discard data
            if (header.length > 0) {
                std::vector<uint32_t> dummy;
                int ret = io_.readData(dummy, header.length);
                if (ret != 0) {
                    return ret;
                }
            }
            return 0;
    }
}

int VTestClient::handleProtocolVersion(const VTestHeader& header) {
    uint32_t client_version;
    int ret = io_.readData(&client_version, sizeof(client_version));
    if (ret != 0) {
        return ret;
    }

    LOGI("Client protocol version: %u", client_version);

    uint32_t server_version = VTEST_PROTOCOL_VERSION;
    return io_.writeResponse(VCMD_PROTOCOL_VERSION, &server_version, sizeof(server_version));
}

int VTestClient::handleGetCaps2(const VTestHeader& header) {
    uint32_t params[2];
    int ret = io_.readData(params, sizeof(params));
    if (ret != 0) {
        return ret;
    }

    uint32_t capset_id = params[0];
    uint32_t capset_version = params[1];

    LOGD("GET_CAPS2: capset_id=%u, version=%u", capset_id, capset_version);

    uint32_t max_ver, max_size;
    virgl_renderer_get_cap_set(capset_id, &max_ver, &max_size);

    if (max_size == 0) {
        LOGW("No capabilities for capset %u", capset_id);
        uint32_t valid = 0;
        return io_.writeResponse(VCMD_GET_CAPS2, &valid, sizeof(valid));
    }

    std::vector<uint8_t> caps_data(max_size + sizeof(uint32_t));
    uint32_t* response = reinterpret_cast<uint32_t*>(caps_data.data());
    response[0] = 1;  // valid

    virgl_renderer_fill_caps(capset_id, max_ver, &response[1]);

    return io_.writeResponse(VCMD_GET_CAPS2, caps_data.data(), caps_data.size());
}

int VTestClient::handleGetCapset(const VTestHeader& header) {
    uint32_t params[2];
    int ret = io_.readData(params, sizeof(params));
    if (ret != 0) {
        return ret;
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
    uint32_t* response = reinterpret_cast<uint32_t*>(caps_data.data());
    response[0] = 1;  // valid

    virgl_renderer_fill_caps(capset_id, max_ver, &response[1]);

    return io_.writeResponse(VCMD_GET_CAPSET, caps_data.data(), caps_data.size());
}

int VTestClient::handleContextInit(const VTestHeader& header) {
    uint32_t capset_id;
    int ret = io_.readData(&capset_id, sizeof(capset_id));
    if (ret != 0) {
        return ret;
    }

    LOGI("CONTEXT_INIT: capset_id=%u", capset_id);

    // Allocate unique context ID
    virgl_ctx_id_ = __atomic_fetch_add(&next_ctx_id_, 1, __ATOMIC_SEQ_CST);

    // Create virgl context
    const char* name = "vtest";
    int vret = virgl_renderer_context_create_with_flags(
        virgl_ctx_id_, capset_id, strlen(name), name);

    if (vret != 0) {
        LOGE("virgl_renderer_context_create_with_flags failed: %d", vret);
        return -EIO;
    }

    LOGI("VirGL context %u created successfully", virgl_ctx_id_);
    return 0;  // No response
}

int VTestClient::handleResourceCreateBlob(const VTestHeader& header) {
    uint32_t params[6];
    int ret = io_.readData(params, sizeof(params));
    if (ret != 0) {
        return ret;
    }

    uint32_t blob_type = params[0];
    uint32_t blob_flags = params[1];
    uint64_t size = (static_cast<uint64_t>(params[3]) << 32) | params[2];
    uint64_t blob_id = (static_cast<uint64_t>(params[5]) << 32) | params[4];

    LOGD("RESOURCE_CREATE_BLOB: type=%u, flags=0x%x, size=%llu, id=%llu",
         blob_type, blob_flags, (unsigned long long)size, (unsigned long long)blob_id);

    struct virgl_renderer_resource_create_blob_args args = {};
    args.res_handle = 0;  // virgl will allocate
    args.ctx_id = virgl_ctx_id_;
    args.blob_mem = blob_type;
    args.blob_flags = blob_flags;
    args.size = size;
    args.blob_id = blob_id;

    int vret = virgl_renderer_resource_create_blob(&args);
    if (vret != 0) {
        LOGE("virgl_renderer_resource_create_blob failed: %d", vret);
        return -EIO;
    }

    uint32_t res_id = args.res_handle;
    resources_[res_id] = res_id;

    LOGD("Resource created: res_id=%u", res_id);
    return io_.writeResponse(VCMD_RESOURCE_CREATE_BLOB, &res_id, sizeof(res_id));
}

int VTestClient::handleResourceUnref(const VTestHeader& header) {
    uint32_t res_id;
    int ret = io_.readData(&res_id, sizeof(res_id));
    if (ret != 0) {
        return ret;
    }

    LOGD("RESOURCE_UNREF: res_id=%u", res_id);

    auto it = resources_.find(res_id);
    if (it != resources_.end()) {
        virgl_renderer_resource_unref(it->second);
        resources_.erase(it);
    }

    return 0;  // No response
}

int VTestClient::handleSubmitCmd(const VTestHeader& header) {
    std::vector<uint32_t> cmd_buf;
    int ret = io_.readData(cmd_buf, header.length);
    if (ret != 0) {
        return ret;
    }

    LOGD("SUBMIT_CMD: size=%u dwords", header.length);

    // Make context current before submitting
    if (!EGLManager::instance()->makeCurrent(egl_ctx_, egl_surface_)) {
        LOGE("Failed to make context current");
        return -EIO;
    }

    // Submit commands to virglrenderer
    int vret = virgl_renderer_submit_cmd(cmd_buf.data(), virgl_ctx_id_, header.length);
    if (vret != 0) {
        LOGE("virgl_renderer_submit_cmd failed: %d", vret);
        return -EIO;
    }

    // Swap buffers if window surface
    if (native_window_) {
        eglSwapBuffers(EGLManager::instance()->getDisplay(), egl_surface_);
    }

    return 0;  // No response
}

int VTestClient::handleTransferPut2(const VTestHeader& header) {
    uint32_t params[10];
    int ret = io_.readData(params, sizeof(params));
    if (ret != 0) {
        return ret;
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
    uint64_t offset = params[9];

    LOGD("TRANSFER_PUT2: res=%u, level=%u, box=(%u,%u,%u,%u,%u,%u), size=%u",
         res_id, level, x, y, z, width, height, depth, data_size);

    // Read data
    std::vector<uint8_t> data(data_size);
    if (data_size > 0) {
        ret = io_.readData(data.data(), data_size);
        if (ret != 0) {
            return ret;
        }
    }

    // Transfer to virgl
    struct iovec iov = { data.data(), data_size };
    struct virgl_box box = { x, y, z, width, height, depth };

    int vret = virgl_renderer_transfer_write_iov(
        res_id, virgl_ctx_id_, level, 0, 0, &box, offset, &iov, 1);

    if (vret != 0) {
        LOGE("virgl_renderer_transfer_write_iov failed: %d", vret);
        return -EIO;
    }

    return 0;  // No response
}

int VTestClient::handleTransferGet2(const VTestHeader& header) {
    uint32_t params[10];
    int ret = io_.readData(params, sizeof(params));
    if (ret != 0) {
        return ret;
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
    uint64_t offset = params[9];

    LOGD("TRANSFER_GET2: res=%u, level=%u, box=(%u,%u,%u,%u,%u,%u), size=%u",
         res_id, level, x, y, z, width, height, depth, data_size);

    // Prepare buffer
    std::vector<uint8_t> data(data_size);
    struct iovec iov = { data.data(), data_size };
    struct virgl_box box = { x, y, z, width, height, depth };

    int vret = virgl_renderer_transfer_read_iov(
        res_id, virgl_ctx_id_, level, 0, 0, &box, offset, &iov, 1);

    if (vret != 0) {
        LOGE("virgl_renderer_transfer_read_iov failed: %d", vret);
        return -EIO;
    }

    // Send data back to client
    return io_.writeResponse(VCMD_TRANSFER_GET2, data.data(), data.size());
}

void VTestClient::setEGLSurface(EGLSurface surface, ANativeWindow* window) {
    std::lock_guard<std::mutex> lock(surface_mutex_);

    // Destroy old surface
    if (egl_surface_ != EGL_NO_SURFACE) {
        EGLManager::instance()->destroySurface(egl_surface_);
        egl_surface_ = EGL_NO_SURFACE;
    }

    // Release old window
    if (native_window_) {
        ANativeWindow_release(native_window_);
        native_window_ = nullptr;
    }

    // Set new surface
    egl_surface_ = surface;

    // Acquire new window
    if (window) {
        native_window_ = window;
        ANativeWindow_acquire(native_window_);
    }

    LOGI("EGL surface updated: surface=%p, window=%p", surface, window);
}
