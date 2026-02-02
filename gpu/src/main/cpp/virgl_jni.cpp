/**
 * VirGL JNI Implementation
 *
 * Provides JNI bindings for virglrenderer with Android EGL integration.
 * Implements vtest protocol support for GPU virtualization.
 */

#include <jni.h>
#include <pthread.h>
#include <cstring>
#include <unistd.h>
#include <sys/uio.h>
#include <android/log.h>
#include <android/native_window_jni.h>
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <unordered_map>
#include <memory>
#include <mutex>

extern "C" {
#include "virglrenderer/src/virglrenderer.h"
#include "virglrenderer/src/virgl_hw.h"
#include "virglrenderer/src/drm/drm-uapi/virtgpu_drm.h"
}

#include "virgl_jni_utils.h"
#include "virgl_egl_manager.h"
#include "virgl_context.h"

// ============================================================================
// Global State
// ============================================================================

JavaVM* g_jvm = nullptr;
thread_local JNIEnv* tls_env = nullptr;

std::unordered_map<uint64_t, std::unique_ptr<VirGLContext>> g_contexts;
std::mutex g_contexts_mutex;

static struct virgl_renderer_callbacks g_callbacks;
static bool g_renderer_initialized = false;

// ============================================================================
// JNI Utilities Implementation
// ============================================================================

JNIEnv* getJNIEnv() {
    if (tls_env) {
        return tls_env;
    }

    JNIEnv* env = nullptr;
    jint result = g_jvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);

    if (result == JNI_EDETACHED) {
        // Thread not attached, attach it
        JavaVMAttachArgs args = {
            JNI_VERSION_1_6,
            "VirGL-Native-Thread",
            nullptr
        };
        result = g_jvm->AttachCurrentThread(&env, &args);
        if (result != JNI_OK) {
            LOGE("Failed to attach thread: %d", result);
            return nullptr;
        }
    }

    tls_env = env;
    return env;
}

void throwJavaException(JNIEnv* env, const char* className, const char* message) {
    jclass exClass = env->FindClass(className);
    if (exClass != nullptr) {
        env->ThrowNew(exClass, message);
        env->DeleteLocalRef(exClass);
    }
}

// ============================================================================
// VirGLContext Implementation
// ============================================================================

VirGLContext::~VirGLContext() {
    // Clean up EGL resources
    if (egl_ctx != EGL_NO_CONTEXT) {
        EGLManager::instance()->destroyContext(egl_ctx);
    }
    if (egl_surface != EGL_NO_SURFACE) {
        EGLManager::instance()->destroySurface(egl_surface);
    }

    // Release native window
    if (native_window) {
        ANativeWindow_release(native_window);
        native_window = nullptr;
    }

    // Delete global ref to Surface
    if (surface_ref) {
        JNIEnv* env = getJNIEnv();
        if (env) {
            env->DeleteGlobalRef(surface_ref);
            surface_ref = nullptr;
        }
    }

    // Delete global ref to VirGLRenderer
    if (renderer_ref) {
        JNIEnv* env = getJNIEnv();
        if (env) {
            env->DeleteGlobalRef(renderer_ref);
            renderer_ref = nullptr;
        }
    }
}

VirGLContext* getContext(uint64_t renderer_id) {
    std::lock_guard<std::mutex> lock(g_contexts_mutex);
    auto it = g_contexts.find(renderer_id);
    if (it == g_contexts.end()) {
        return nullptr;
    }
    return it->second.get();
}

// ============================================================================
// EGLManager Implementation
// ============================================================================

EGLManager* EGLManager::instance() {
    static EGLManager mgr;
    return &mgr;
}

EGLManager::EGLManager() = default;
EGLManager::~EGLManager() {
    if (display_ != EGL_NO_DISPLAY) {
        eglTerminate(display_);
    }
}

bool EGLManager::initialize() {
    if (initialized_) {
        return true;
    }

    // Get default display
    display_ = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (display_ == EGL_NO_DISPLAY) {
        LOGE("eglGetDisplay failed");
        return false;
    }

    // Initialize EGL
    EGLint major, minor;
    if (!eglInitialize(display_, &major, &minor)) {
        LOGE("eglInitialize failed");
        return false;
    }
    LOGI("EGL initialized: version %d.%d", major, minor);

    // Choose config
    EGLint attribs[] = {
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
        EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
        EGL_BLUE_SIZE, 8,
        EGL_GREEN_SIZE, 8,
        EGL_RED_SIZE, 8,
        EGL_ALPHA_SIZE, 8,
        EGL_DEPTH_SIZE, 24,
        EGL_STENCIL_SIZE, 8,
        EGL_NONE
    };

    EGLint num_config;
    if (!eglChooseConfig(display_, attribs, &config_, 1, &num_config) || num_config == 0) {
        LOGE("eglChooseConfig failed");
        return false;
    }

    initialized_ = true;
    return true;
}

EGLContext EGLManager::createContext() {
    if (!initialized_ && !initialize()) {
        return EGL_NO_CONTEXT;
    }

    EGLint ctx_attribs[] = {
        EGL_CONTEXT_CLIENT_VERSION, 3,
        EGL_NONE
    };

    EGLContext ctx = eglCreateContext(display_, config_, EGL_NO_CONTEXT, ctx_attribs);
    if (ctx == EGL_NO_CONTEXT) {
        LOGE("eglCreateContext failed: 0x%x", eglGetError());
        return EGL_NO_CONTEXT;
    }

    return ctx;
}

EGLSurface EGLManager::createPbufferSurface(int width, int height) {
    if (!initialized_ && !initialize()) {
        return EGL_NO_SURFACE;
    }

    EGLint pbuffer_attribs[] = {
        EGL_WIDTH, width,
        EGL_HEIGHT, height,
        EGL_NONE
    };

    EGLSurface surface = eglCreatePbufferSurface(display_, config_, pbuffer_attribs);
    if (surface == EGL_NO_SURFACE) {
        LOGE("eglCreatePbufferSurface failed: 0x%x", eglGetError());
        return EGL_NO_SURFACE;
    }

    return surface;
}

EGLSurface EGLManager::createWindowSurface(ANativeWindow* window) {
    if (!initialized_ && !initialize()) {
        return EGL_NO_SURFACE;
    }

    if (!window) {
        LOGE("createWindowSurface: null window");
        return EGL_NO_SURFACE;
    }

    EGLSurface surface = eglCreateWindowSurface(display_, config_, window, nullptr);
    if (surface == EGL_NO_SURFACE) {
        LOGE("eglCreateWindowSurface failed: 0x%x", eglGetError());
        return EGL_NO_SURFACE;
    }

    LOGD("Window surface created: %p", surface);
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
    if (!eglMakeCurrent(display_, surface, surface, context)) {
        LOGE("eglMakeCurrent failed: 0x%x", eglGetError());
        return false;
    }
    return true;
}

// ============================================================================
// VirGL Callbacks (v4)
// ============================================================================

static void* get_egl_display_callback(void* cookie) {
    EGLDisplay display = EGLManager::instance()->getDisplay();
    LOGD("get_egl_display_callback: %p", display);
    return reinterpret_cast<void*>(display);
}

static virgl_renderer_gl_context create_gl_context_callback(
    void* cookie, int scanout_idx, struct virgl_renderer_gl_ctx_param* param) {

    auto* ctx = static_cast<VirGLContext*>(cookie);
    LOGD("create_gl_context_callback: renderer_id=%lld, major=%d",
         (long long)ctx->renderer_id, param->major_ver);

    // Create EGL context
    EGLContext egl_ctx = EGLManager::instance()->createContext();
    if (egl_ctx == EGL_NO_CONTEXT) {
        LOGE("Failed to create EGL context");
        return nullptr;
    }

    // Create window surface from native window (already set in createContext)
    EGLSurface egl_surface = EGL_NO_SURFACE;
    if (ctx->native_window) {
        egl_surface = EGLManager::instance()->createWindowSurface(ctx->native_window);
        if (egl_surface == EGL_NO_SURFACE) {
            EGLManager::instance()->destroyContext(egl_ctx);
            LOGE("Failed to create EGL window surface");
            return nullptr;
        }
        LOGD("Created window surface from ANativeWindow");
    } else {
        // Fallback to pbuffer if no window available
        egl_surface = EGLManager::instance()->createPbufferSurface(1280, 720);
        if (egl_surface == EGL_NO_SURFACE) {
            EGLManager::instance()->destroyContext(egl_ctx);
            LOGE("Failed to create EGL surface");
            return nullptr;
        }
        LOGW("No native window, using pbuffer as fallback");
    }

    ctx->egl_ctx = egl_ctx;
    ctx->egl_surface = egl_surface;

    LOGD("EGL context created: ctx=%p, surface=%p", egl_ctx, egl_surface);
    return reinterpret_cast<virgl_renderer_gl_context>(egl_ctx);
}

static void destroy_gl_context_callback(void* cookie, virgl_renderer_gl_context ctx) {
    LOGD("destroy_gl_context_callback: ctx=%p", ctx);
    EGLContext egl_ctx = reinterpret_cast<EGLContext>(ctx);
    EGLManager::instance()->destroyContext(egl_ctx);
}

static int make_current_callback(void* cookie, int scanout_idx, virgl_renderer_gl_context ctx) {
    auto* virgl_ctx = static_cast<VirGLContext*>(cookie);

    if (ctx == nullptr) {
        // Unbind context
        return EGLManager::instance()->makeCurrent(EGL_NO_CONTEXT, EGL_NO_SURFACE) ? 0 : -1;
    }

    EGLContext egl_ctx = reinterpret_cast<EGLContext>(ctx);
    bool success = EGLManager::instance()->makeCurrent(egl_ctx, virgl_ctx->egl_surface);
    return success ? 0 : -1;
}

static void write_fence_callback(void* cookie, uint32_t fence_id) {
    auto* ctx = static_cast<VirGLContext*>(cookie);
    LOGD("write_fence_callback: renderer_id=%lu, fence_id=%u", ctx->renderer_id, fence_id);

    if (!ctx->renderer_ref) {
        return;
    }

    JNIEnv* env = getJNIEnv();
    if (!env) {
        LOGE("Failed to get JNIEnv for fence callback");
        return;
    }

    // Call Java method: VirGLRenderer.onFenceSignaled(int)
    jclass cls = env->GetObjectClass(ctx->renderer_ref);
    jmethodID mid = env->GetMethodID(cls, "onFenceSignaled", "(I)V");
    if (mid) {
        env->CallVoidMethod(ctx->renderer_ref, mid, static_cast<jint>(fence_id));
    } else {
        LOGE("Failed to find onFenceSignaled method");
    }
    env->DeleteLocalRef(cls);
}

// ============================================================================
// JNI Methods
// ============================================================================

extern "C" {

JNIEXPORT jint JNICALL
Java_com_github_andock_gpu_virgl_VirGLNative_initRenderer(JNIEnv* env, jclass, jint flags) {
    if (g_renderer_initialized) {
        LOGW("Renderer already initialized");
        return 0;
    }

    // Initialize EGL
    if (!EGLManager::instance()->initialize()) {
        LOGE("Failed to initialize EGL");
        return -1;
    }

    // Setup callbacks (v4 with external EGL)
    memset(&g_callbacks, 0, sizeof(g_callbacks));
    g_callbacks.version = 4;
    g_callbacks.get_egl_display = get_egl_display_callback;
    g_callbacks.create_gl_context = create_gl_context_callback;
    g_callbacks.destroy_gl_context = destroy_gl_context_callback;
    g_callbacks.make_current = make_current_callback;
    g_callbacks.write_fence = write_fence_callback;

    // Initialize virglrenderer
    int ret = virgl_renderer_init(nullptr, flags, &g_callbacks);
    if (ret != 0) {
        LOGE("virgl_renderer_init failed: %d", ret);
        return ret;
    }

    g_renderer_initialized = true;
    LOGI("VirGL renderer initialized successfully with flags=0x%x", flags);
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_github_andock_gpu_virgl_VirGLNative_createContext(
    JNIEnv* env, jclass, jlong rendererId, jobject renderer, jobject surface) {

    LOGD("createContext: rendererId=%lld", (long long)rendererId);

    // Generate unique context ID from rendererId
    int ctxId = (int)(rendererId & 0xFFFF);

    // Get native window from Surface
    ANativeWindow* nativeWindow = ANativeWindow_fromSurface(env, surface);
    if (!nativeWindow) {
        LOGE("Failed to get ANativeWindow from Surface");
        return -EINVAL;
    }

    LOGD("Got ANativeWindow: %p", nativeWindow);

    // Create context structure
    auto ctx = std::make_unique<VirGLContext>(rendererId, ctxId);
    ctx->thread_id = pthread_self();
    ctx->native_window = nativeWindow;
    ctx->surface_ref = env->NewGlobalRef(surface);     // Keep Surface alive
    ctx->renderer_ref = env->NewGlobalRef(renderer);   // Keep VirGLRenderer alive for callbacks

    // Store context (no virgl context created yet - wait for VCMD_CONTEXT_INIT)
    {
        std::lock_guard<std::mutex> lock(g_contexts_mutex);
        g_contexts[rendererId] = std::move(ctx);
    }

    LOGI("Context pre-initialized: rendererId=%lld, ctxId=%d, window=%p",
         (long long)rendererId, ctxId, nativeWindow);
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_github_andock_gpu_virgl_VirGLNative_initContext(
    JNIEnv* env, jclass, jlong rendererId, jint capsetId) {

    LOGD("initContext: rendererId=%lld, capsetId=%d", (long long)rendererId, capsetId);

    VirGLContext* ctx = getContext(rendererId);
    if (!ctx) {
        LOGE("Context not found for renderer %lld", (long long)rendererId);
        return -EINVAL;
    }

    // Create virgl context (will trigger create_gl_context_callback)
    int ret = virgl_renderer_context_create_with_flags(
        ctx->ctx_id, capsetId, strlen("virgl"), "virgl");
    if (ret != 0) {
        LOGE("virgl_renderer_context_create_with_flags failed: %d", ret);
        return ret;
    }

    LOGI("Virgl context initialized: rendererId=%lld, ctxId=%d, capsetId=%d",
         (long long)rendererId, ctx->ctx_id, capsetId);
    return 0;
}

JNIEXPORT void JNICALL
Java_com_github_andock_gpu_virgl_VirGLNative_destroyContext(
    JNIEnv* env, jclass, jlong rendererId) {

    LOGD("destroyContext: rendererId=%lu", rendererId);

    VirGLContext* ctx = getContext(rendererId);
    if (!ctx) {
        LOGW("Context not found: %lu", rendererId);
        return;
    }

    // Destroy virgl context
    virgl_renderer_context_destroy(ctx->ctx_id);

    // Remove from map (will trigger VirGLContext destructor)
    {
        std::lock_guard<std::mutex> lock(g_contexts_mutex);
        g_contexts.erase(rendererId);
    }

    LOGI("Context destroyed: rendererId=%lu", rendererId);
}

JNIEXPORT jobject JNICALL
Java_com_github_andock_gpu_virgl_VirGLNative_getCaps(JNIEnv* env, jclass) {
    // Get capabilities
    uint32_t max_ver, max_size;
    virgl_renderer_get_cap_set(VIRTGPU_DRM_CAPSET_VIRGL2, &max_ver, &max_size);

    if (max_size == 0) {
        LOGW("No capabilities available");
        return env->NewDirectByteBuffer(nullptr, 0);
    }

    void* caps_data = malloc(max_size);
    if (!caps_data) {
        LOGE("Failed to allocate caps buffer");
        return nullptr;
    }

    virgl_renderer_fill_caps(VIRTGPU_DRM_CAPSET_VIRGL2, max_ver, caps_data);

    // Create DirectByteBuffer (will be freed by Java)
    jobject buffer = env->NewDirectByteBuffer(caps_data, max_size);
    return buffer;
}

// ============================================================================
// Stage 4: Blob Resource Management
// ============================================================================

JNIEXPORT jint JNICALL
Java_com_github_andock_gpu_virgl_VirGLNative_createResourceBlob(
    JNIEnv* env, jclass, jlong rendererId, jobject argsObj) {

    VirGLContext* ctx = getContext(rendererId);
    if (!ctx) {
        LOGE("createResourceBlob: context not found");
        return -EINVAL;
    }

    // Extract blob args from Java object
    jclass argsClass = env->GetObjectClass(argsObj);
    jfieldID resIdField = env->GetFieldID(argsClass, "resId", "I");
    jfieldID blobTypeField = env->GetFieldID(argsClass, "blobType", "I");
    jfieldID blobFlagsField = env->GetFieldID(argsClass, "blobFlags", "I");
    jfieldID blobIdField = env->GetFieldID(argsClass, "blobId", "J");
    jfieldID sizeField = env->GetFieldID(argsClass, "size", "J");

    jint resId = env->GetIntField(argsObj, resIdField);
    jint blobType = env->GetIntField(argsObj, blobTypeField);
    jint blobFlags = env->GetIntField(argsObj, blobFlagsField);
    jlong blobId = env->GetLongField(argsObj, blobIdField);
    jlong size = env->GetLongField(argsObj, sizeField);

    LOGD("createResourceBlob: resId=%d, type=%d, flags=0x%x, blobId=%lld, size=%lld",
         resId, blobType, blobFlags, (long long)blobId, (long long)size);

    // Call virglrenderer
    struct virgl_renderer_resource_create_blob_args blob_args = {};
    blob_args.res_handle = resId;
    blob_args.ctx_id = ctx->ctx_id;
    blob_args.blob_mem = blobType;
    blob_args.blob_flags = blobFlags;
    blob_args.blob_id = blobId;
    blob_args.size = size;

    int ret = virgl_renderer_resource_create_blob(&blob_args);
    if (ret != 0) {
        LOGE("virgl_renderer_resource_create_blob failed: %d", ret);
        return ret;
    }

    // Track resource
    {
        std::lock_guard<std::mutex> lock(ctx->resources_mutex);
        ResourceInfo info = {};
        info.res_id = resId;
        info.blob_mem = blobType;
        info.size = size;
        info.exported_fd = -1;
        ctx->resources[resId] = info;
    }

    return 0;
}

JNIEXPORT jint JNICALL
Java_com_github_andock_gpu_virgl_VirGLNative_exportResourceBlob(
    JNIEnv* env, jclass, jlong rendererId, jint resId) {

    VirGLContext* ctx = getContext(rendererId);
    if (!ctx) {
        LOGE("exportResourceBlob: context not found");
        return -EINVAL;
    }

    LOGD("exportResourceBlob: resId=%d", resId);

    uint32_t fd_type = 0;
    int fd = -1;
    int ret = virgl_renderer_resource_export_blob(resId, &fd_type, &fd);
    if (ret != 0) {
        LOGE("virgl_renderer_resource_export_blob failed: %d", ret);
        return ret;
    }

    // Store FD
    {
        std::lock_guard<std::mutex> lock(ctx->resources_mutex);
        auto it = ctx->resources.find(resId);
        if (it != ctx->resources.end()) {
            it->second.exported_fd = fd;
        }
    }

    LOGD("Exported resource %d as fd=%d, type=%d", resId, fd, fd_type);
    return fd;
}

JNIEXPORT jint JNICALL
Java_com_github_andock_gpu_virgl_VirGLNative_destroyResource(
    JNIEnv* env, jclass, jlong rendererId, jint resId) {

    VirGLContext* ctx = getContext(rendererId);
    if (!ctx) {
        LOGE("destroyResource: context not found");
        return -EINVAL;
    }

    LOGD("destroyResource: resId=%d", resId);

    // Close exported FD if any
    {
        std::lock_guard<std::mutex> lock(ctx->resources_mutex);
        auto it = ctx->resources.find(resId);
        if (it != ctx->resources.end()) {
            if (it->second.exported_fd >= 0) {
                close(it->second.exported_fd);
            }
            ctx->resources.erase(it);
        }
    }

    virgl_renderer_resource_unref(resId);
    return 0;
}

// ============================================================================
// Stage 5: Command Submission
// ============================================================================

JNIEXPORT jint JNICALL
Java_com_github_andock_gpu_virgl_VirGLNative_submitCmd(
    JNIEnv* env, jclass, jlong rendererId, jobject cmdBuffer) {

    VirGLContext* ctx = getContext(rendererId);
    if (!ctx) {
        LOGE("submitCmd: context not found");
        return -EINVAL;
    }

    void* cmd_data = env->GetDirectBufferAddress(cmdBuffer);
    jlong cmd_size = env->GetDirectBufferCapacity(cmdBuffer);

    if (!cmd_data || cmd_size == 0) {
        LOGE("submitCmd: invalid buffer");
        return -EINVAL;
    }

    int ndw = cmd_size / 4;
    LOGD("submitCmd: ctx=%d, ndw=%d", ctx->ctx_id, ndw);

    int ret = virgl_renderer_submit_cmd(cmd_data, ctx->ctx_id, ndw);
    if (ret != 0) {
        LOGE("virgl_renderer_submit_cmd failed: %d", ret);
        return ret;
    }

    return 0;
}

JNIEXPORT jint JNICALL
Java_com_github_andock_gpu_virgl_VirGLNative_submitCmd2(
    JNIEnv* env, jclass, jlong rendererId, jobject cmdBuffer, jobject argsObj) {

    VirGLContext* ctx = getContext(rendererId);
    if (!ctx) {
        LOGE("submitCmd2: context not found");
        return -EINVAL;
    }

    void* cmd_data = env->GetDirectBufferAddress(cmdBuffer);
    jlong cmd_size = env->GetDirectBufferCapacity(cmdBuffer);

    if (!cmd_data || cmd_size == 0) {
        LOGE("submitCmd2: invalid buffer");
        return -EINVAL;
    }

    int ndw = cmd_size / 4;
    LOGD("submitCmd2: ctx=%d, ndw=%d", ctx->ctx_id, ndw);

    // For now, just submit without sync handling (simplified)
    int ret = virgl_renderer_submit_cmd(cmd_data, ctx->ctx_id, ndw);
    if (ret != 0) {
        LOGE("virgl_renderer_submit_cmd failed: %d", ret);
        return ret;
    }

    return 0;
}

// ============================================================================
// Stage 6: Synchronization
// ============================================================================

JNIEXPORT jint JNICALL
Java_com_github_andock_gpu_virgl_VirGLNative_createSync(
    JNIEnv* env, jclass, jlong rendererId, jint syncId, jlong initialValue) {

    VirGLContext* ctx = getContext(rendererId);
    if (!ctx) {
        LOGE("createSync: context not found");
        return -EINVAL;
    }

    LOGD("createSync: syncId=%d, initialValue=%lld", syncId, (long long)initialValue);

    // Track sync object
    {
        std::lock_guard<std::mutex> lock(ctx->syncs_mutex);
        SyncInfo info = {};
        info.sync_id = syncId;
        info.current_value = initialValue;
        info.eventfd = -1;
        ctx->syncs[syncId] = info;
    }

    return 0;
}

JNIEXPORT jint JNICALL
Java_com_github_andock_gpu_virgl_VirGLNative_writeSync(
    JNIEnv* env, jclass, jlong rendererId, jint syncId, jlong value) {

    VirGLContext* ctx = getContext(rendererId);
    if (!ctx) {
        LOGE("writeSync: context not found");
        return -EINVAL;
    }

    LOGD("writeSync: syncId=%d, value=%lld", syncId, (long long)value);

    // Update sync value
    {
        std::lock_guard<std::mutex> lock(ctx->syncs_mutex);
        auto it = ctx->syncs.find(syncId);
        if (it != ctx->syncs.end()) {
            it->second.current_value = value;
        }
    }

    return 0;
}

JNIEXPORT jint JNICALL
Java_com_github_andock_gpu_virgl_VirGLNative_syncWait(
    JNIEnv* env, jclass, jlong rendererId, jintArray syncIdsArray,
    jlongArray valuesArray, jlong timeoutNs) {

    VirGLContext* ctx = getContext(rendererId);
    if (!ctx) {
        LOGE("syncWait: context not found");
        return -EINVAL;
    }

    jsize numSyncs = env->GetArrayLength(syncIdsArray);
    jint* syncIds = env->GetIntArrayElements(syncIdsArray, nullptr);
    jlong* values = env->GetLongArrayElements(valuesArray, nullptr);

    LOGD("syncWait: numSyncs=%d, timeout=%lld ns", numSyncs, (long long)timeoutNs);

    // Simple polling implementation (no eventfd on Android < 26)
    bool all_reached = false;
    long long start_time = 0; // TODO: implement timeout
    int result = 0;

    {
        std::lock_guard<std::mutex> lock(ctx->syncs_mutex);
        all_reached = true;
        for (int i = 0; i < numSyncs; i++) {
            auto it = ctx->syncs.find(syncIds[i]);
            if (it == ctx->syncs.end() || it->second.current_value < (uint64_t)values[i]) {
                all_reached = false;
                break;
            }
        }
    }

    if (!all_reached) {
        result = -ETIMEDOUT;
    }

    env->ReleaseIntArrayElements(syncIdsArray, syncIds, JNI_ABORT);
    env->ReleaseLongArrayElements(valuesArray, values, JNI_ABORT);

    return result;
}

JNIEXPORT jint JNICALL
Java_com_github_andock_gpu_virgl_VirGLNative_destroySync(
    JNIEnv* env, jclass, jlong rendererId, jint syncId) {

    VirGLContext* ctx = getContext(rendererId);
    if (!ctx) {
        LOGE("destroySync: context not found");
        return -EINVAL;
    }

    LOGD("destroySync: syncId=%d", syncId);

    {
        std::lock_guard<std::mutex> lock(ctx->syncs_mutex);
        auto it = ctx->syncs.find(syncId);
        if (it != ctx->syncs.end()) {
            if (it->second.eventfd >= 0) {
                close(it->second.eventfd);
            }
            ctx->syncs.erase(it);
        }
    }

    return 0;
}

// ============================================================================
// Stage 7: Transfer Operations
// ============================================================================

JNIEXPORT jint JNICALL
Java_com_github_andock_gpu_virgl_VirGLNative_transferPut(
    JNIEnv* env, jclass, jlong rendererId, jint resId, jint level,
    jint x, jint y, jint z, jint width, jint height, jint depth,
    jint stride, jint layerStride, jobject dataBuffer) {

    VirGLContext* ctx = getContext(rendererId);
    if (!ctx) {
        LOGE("transferPut: context not found");
        return -EINVAL;
    }

    void* data = env->GetDirectBufferAddress(dataBuffer);
    jlong data_size = env->GetDirectBufferCapacity(dataBuffer);

    if (!data || data_size == 0) {
        LOGE("transferPut: invalid buffer");
        return -EINVAL;
    }

    LOGD("transferPut: resId=%d, level=%d, box=(%d,%d,%d,%d,%d,%d)",
         resId, level, x, y, z, width, height, depth);

    struct virgl_box box = {};
    box.x = x;
    box.y = y;
    box.z = z;
    box.w = width;
    box.h = height;
    box.d = depth;

    struct iovec iov = {};
    iov.iov_base = data;
    iov.iov_len = data_size;

    int ret = virgl_renderer_transfer_write_iov(
        resId, ctx->ctx_id, level, stride, layerStride, &box, 0, &iov, 1);
    if (ret != 0) {
        LOGE("virgl_renderer_transfer_write_iov failed: %d", ret);
        return ret;
    }

    return 0;
}

JNIEXPORT jint JNICALL
Java_com_github_andock_gpu_virgl_VirGLNative_transferGet(
    JNIEnv* env, jclass, jlong rendererId, jint resId, jint level,
    jint x, jint y, jint z, jint width, jint height, jint depth,
    jint stride, jint layerStride, jobject dataBuffer) {

    VirGLContext* ctx = getContext(rendererId);
    if (!ctx) {
        LOGE("transferGet: context not found");
        return -EINVAL;
    }

    void* data = env->GetDirectBufferAddress(dataBuffer);
    jlong data_size = env->GetDirectBufferCapacity(dataBuffer);

    if (!data || data_size == 0) {
        LOGE("transferGet: invalid buffer");
        return -EINVAL;
    }

    LOGD("transferGet: resId=%d, level=%d, box=(%d,%d,%d,%d,%d,%d)",
         resId, level, x, y, z, width, height, depth);

    struct virgl_box box = {};
    box.x = x;
    box.y = y;
    box.z = z;
    box.w = width;
    box.h = height;
    box.d = depth;

    struct iovec iov = {};
    iov.iov_base = data;
    iov.iov_len = data_size;

    int ret = virgl_renderer_transfer_read_iov(
        resId, ctx->ctx_id, level, stride, layerStride, &box, 0, &iov, 1);
    if (ret != 0) {
        LOGE("virgl_renderer_transfer_read_iov failed: %d", ret);
        return ret;
    }

    return 0;
}

} // extern "C"

// ============================================================================
// JNI_OnLoad
// ============================================================================

jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    g_jvm = vm;

    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        LOGE("JNI_OnLoad: GetEnv failed");
        return JNI_ERR;
    }

    LOGI("VirGL JNI loaded successfully");
    return JNI_VERSION_1_6;
}
