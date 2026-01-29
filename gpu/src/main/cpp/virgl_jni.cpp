/**
 * VirGL JNI Wrapper
 *
 * Provides JNI bindings for virglrenderer API.
 * Uses pthread_key for thread management.
 * Global callback state (virgl_renderer_init is a global initialization function).
 */

#include <jni.h>
#include <pthread.h>
#include <cstring>
#include <sys/uio.h>
#include <android/log.h>

extern "C" {
#include "virglrenderer/src/virglrenderer.h"
#include "virglrenderer/src/virgl_hw.h"
}

#define LOG_TAG "VirGL_JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// ============================================================================
// Global State
// ============================================================================

static JavaVM *g_jvm = nullptr;
static pthread_key_t g_thread_key;
static bool g_thread_key_initialized = false;

// Global callback state (virglrenderer is single-instance)
static jobject g_callback = nullptr;
static jmethodID g_onWriteFence = nullptr;
static jmethodID g_onCreateGLContext = nullptr;
static jmethodID g_onDestroyGLContext = nullptr;
static jmethodID g_onMakeCurrent = nullptr;

// Thread destructor - automatically detach when thread exits
static void thread_destructor(void *value) {
    if (value && g_jvm) {
        LOGD("Thread exiting, detaching from JVM");
        g_jvm->DetachCurrentThread();
    }
}

// Get JNIEnv for current thread, attaching if necessary
static JNIEnv *get_env() {
    JNIEnv *env = nullptr;
    if (!g_jvm) {
        LOGE("JVM not initialized");
        return nullptr;
    }

    jint result = g_jvm->GetEnv((void **) &env, JNI_VERSION_1_6);
    if (result == JNI_OK) {
        return env;
    }

    if (result == JNI_EDETACHED) {
        if (g_jvm->AttachCurrentThread(&env, nullptr) == JNI_OK) {
            pthread_setspecific(g_thread_key, (void *) 1);
            LOGD("Attached thread to JVM");
            return env;
        }
        LOGE("Failed to attach thread to JVM");
    }

    return nullptr;
}

// ============================================================================
// Callback Bridge Functions (global)
// ============================================================================

static void write_fence_cb(void *cookie, uint32_t fence) {
    JNIEnv *env = get_env();
    if (env && g_callback) {
        env->CallVoidMethod(g_callback, g_onWriteFence, (jint) fence);
    }
}

static virgl_renderer_gl_context create_gl_context_cb(
        void *cookie, int scanout_idx, struct virgl_renderer_gl_ctx_param *param) {
    JNIEnv *env = get_env();
    if (env && g_callback) {
        jlong ctx = env->CallLongMethod(g_callback, g_onCreateGLContext,
                                        scanout_idx, param->major_ver, param->minor_ver);
        return (virgl_renderer_gl_context) ctx;
    }
    return nullptr;
}

static void destroy_gl_context_cb(void *cookie, virgl_renderer_gl_context ctx) {
    JNIEnv *env = get_env();
    if (env && g_callback) {
        env->CallVoidMethod(g_callback, g_onDestroyGLContext, (jlong) ctx);
    }
}

static int make_current_cb(void *cookie, int scanout_idx, virgl_renderer_gl_context ctx) {
    JNIEnv *env = get_env();
    if (env && g_callback) {
        return env->CallIntMethod(g_callback, g_onMakeCurrent, scanout_idx, (jlong) ctx);
    }
    return -1;
}

static struct virgl_renderer_callbacks g_callbacks = {
        .version = 1,
        .write_fence = write_fence_cb,
        .create_gl_context = create_gl_context_cb,
        .destroy_gl_context = destroy_gl_context_cb,
        .make_current = make_current_cb,
};

// ============================================================================
// JNI_OnLoad
// ============================================================================

extern "C" JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    g_jvm = vm;

    if (!g_thread_key_initialized) {
        if (pthread_key_create(&g_thread_key, thread_destructor) == 0) {
            g_thread_key_initialized = true;
            LOGI("pthread_key created for JNI thread management");
        } else {
            LOGE("Failed to create pthread_key");
        }
    }

    LOGI("VirGL JNI loaded");
    return JNI_VERSION_1_6;
}

// ============================================================================
// JNI Exports
// ============================================================================

extern "C" {

// ----------------------------------------------------------------------------
// Initialization and Cleanup
// ----------------------------------------------------------------------------

static void virgl_legacy_logger(enum virgl_log_level_flags log_level,
                                const char *message,
                                void *user_data) {
    LOGD("virgl:%s", message);
}

JNIEXPORT jint JNICALL
Java_com_github_andock_virgl_VirGL_init(JNIEnv *env, jclass clazz, jint flags, jobject callback) {
    virgl_set_log_callback(virgl_legacy_logger, nullptr, nullptr);
    // Store global callback reference
    if (g_callback) {
        env->DeleteGlobalRef(g_callback);
    }
    g_callback = env->NewGlobalRef(callback);

    // Cache method IDs
    jclass cls = env->GetObjectClass(callback);
    g_onWriteFence = env->GetMethodID(cls, "onWriteFence", "(I)V");
    g_onCreateGLContext = env->GetMethodID(cls, "onCreateGLContext", "(III)J");
    g_onDestroyGLContext = env->GetMethodID(cls, "onDestroyGLContext", "(J)V");
    g_onMakeCurrent = env->GetMethodID(cls, "onMakeCurrent", "(IJ)I");

    if (!g_onWriteFence || !g_onCreateGLContext || !g_onDestroyGLContext || !g_onMakeCurrent) {
        LOGE("Failed to get callback method IDs");
        env->DeleteGlobalRef(g_callback);
        g_callback = nullptr;
        return -1;
    }

    // Initialize virglrenderer
    int ret = virgl_renderer_init(g_callback, flags, &g_callbacks);
    if (ret != 0) {
        LOGE("virgl_renderer_init failed: %d", ret);
        env->DeleteGlobalRef(g_callback);
        g_callback = nullptr;
        return -1;
    }

    LOGI("virgl_renderer_init succeeded");
    return 0;
}

JNIEXPORT void JNICALL
Java_com_github_andock_virgl_VirGL_cleanup(JNIEnv *env, jclass clazz) {
    LOGI("virgl_renderer_cleanup");
    virgl_renderer_cleanup(nullptr);

    if (g_callback) {
        env->DeleteGlobalRef(g_callback);
        g_callback = nullptr;
    }
    g_onWriteFence = nullptr;
    g_onCreateGLContext = nullptr;
    g_onDestroyGLContext = nullptr;
    g_onMakeCurrent = nullptr;
}

// ----------------------------------------------------------------------------
// Context Management
// ----------------------------------------------------------------------------

JNIEXPORT jint JNICALL
Java_com_github_andock_virgl_VirGL_contextCreate(JNIEnv *env, jclass clazz, jint ctxId,
                                                 jstring name) {
    const char *name_str = env->GetStringUTFChars(name, nullptr);
    int ret = virgl_renderer_context_create(ctxId, strlen(name_str), name_str);
    env->ReleaseStringUTFChars(name, name_str);

    if (ret != 0) {
        LOGE("virgl_renderer_context_create failed: %d", ret);
    }
    return ret;
}

JNIEXPORT void JNICALL
Java_com_github_andock_virgl_VirGL_contextDestroy(JNIEnv *env, jclass clazz, jint ctxId) {
    virgl_renderer_context_destroy(ctxId);
}

// ----------------------------------------------------------------------------
// Capability Query
// ----------------------------------------------------------------------------

JNIEXPORT jobject JNICALL
Java_com_github_andock_virgl_VirGL_getCapSet(JNIEnv *env, jclass clazz, jint set) {
    uint32_t max_ver = 0, max_size = 0;
    virgl_renderer_get_cap_set(set, &max_ver, &max_size);

    if (max_size == 0) {
        return nullptr;
    }

    jclass capSetClass = env->FindClass("com/github/andock/virgl/VirGL$CapSet");
    if (!capSetClass) {
        LOGE("Failed to find CapSet class");
        return nullptr;
    }

    jmethodID constructor = env->GetMethodID(capSetClass, "<init>", "(II)V");
    if (!constructor) {
        LOGE("Failed to find CapSet constructor");
        return nullptr;
    }

    return env->NewObject(capSetClass, constructor, (jint) max_ver, (jint) max_size);
}

JNIEXPORT jint JNICALL
Java_com_github_andock_virgl_VirGL_fillCaps(JNIEnv *env, jclass clazz, jint set, jint version,
                                            jobject buffer) {
    void *buf = env->GetDirectBufferAddress(buffer);
    jlong capacity = env->GetDirectBufferCapacity(buffer);

    if (!buf || capacity <= 0) {
        LOGE("Invalid direct buffer for fillCaps");
        return -1;
    }

    uint32_t max_ver = 0, max_size = 0;
    virgl_renderer_get_cap_set(set, &max_ver, &max_size);

    if (max_size == 0 || capacity < max_size) {
        LOGE("Buffer too small for caps: need %u, have %lld", max_size, (long long) capacity);
        return -1;
    }

    virgl_renderer_fill_caps(set, version, buf);
    return 0;
}

// ----------------------------------------------------------------------------
// Resource Management
// ----------------------------------------------------------------------------

JNIEXPORT jint JNICALL
Java_com_github_andock_virgl_VirGL_resourceCreate(JNIEnv *env, jclass clazz, jobject args) {
    jclass argsClass = env->GetObjectClass(args);

    jfieldID handleField = env->GetFieldID(argsClass, "handle", "I");
    jfieldID targetField = env->GetFieldID(argsClass, "target", "I");
    jfieldID formatField = env->GetFieldID(argsClass, "format", "I");
    jfieldID bindField = env->GetFieldID(argsClass, "bind", "I");
    jfieldID widthField = env->GetFieldID(argsClass, "width", "I");
    jfieldID heightField = env->GetFieldID(argsClass, "height", "I");
    jfieldID depthField = env->GetFieldID(argsClass, "depth", "I");
    jfieldID arraySizeField = env->GetFieldID(argsClass, "arraySize", "I");
    jfieldID lastLevelField = env->GetFieldID(argsClass, "lastLevel", "I");
    jfieldID nrSamplesField = env->GetFieldID(argsClass, "nrSamples", "I");
    jfieldID flagsField = env->GetFieldID(argsClass, "flags", "I");

    struct virgl_renderer_resource_create_args native_args = {};
    native_args.handle = env->GetIntField(args, handleField);
    native_args.target = env->GetIntField(args, targetField);
    native_args.format = env->GetIntField(args, formatField);
    native_args.bind = env->GetIntField(args, bindField);
    native_args.width = env->GetIntField(args, widthField);
    native_args.height = env->GetIntField(args, heightField);
    native_args.depth = env->GetIntField(args, depthField);
    native_args.array_size = env->GetIntField(args, arraySizeField);
    native_args.last_level = env->GetIntField(args, lastLevelField);
    native_args.nr_samples = env->GetIntField(args, nrSamplesField);
    native_args.flags = env->GetIntField(args, flagsField);

    int ret = virgl_renderer_resource_create(&native_args, nullptr, 0);
    if (ret != 0) {
        LOGE("virgl_renderer_resource_create failed: %d", ret);
        return 0;
    }

    return native_args.handle;
}

JNIEXPORT void JNICALL
Java_com_github_andock_virgl_VirGL_resourceUnref(JNIEnv *env, jclass clazz, jint handle) {
    virgl_renderer_resource_unref(handle);
}

JNIEXPORT void JNICALL
Java_com_github_andock_virgl_VirGL_ctxAttachResource(JNIEnv *env, jclass clazz, jint ctxId,
                                                     jint resHandle) {
    virgl_renderer_ctx_attach_resource(ctxId, resHandle);
}

JNIEXPORT void JNICALL
Java_com_github_andock_virgl_VirGL_ctxDetachResource(JNIEnv *env, jclass clazz, jint ctxId,
                                                     jint resHandle) {
    virgl_renderer_ctx_detach_resource(ctxId, resHandle);
}

// ----------------------------------------------------------------------------
// Command Submission
// ----------------------------------------------------------------------------

JNIEXPORT jint JNICALL
Java_com_github_andock_virgl_VirGL_submitCmd(JNIEnv *env, jclass clazz, jobject buffer,
                                             jint ctxId) {
    void *buf = env->GetDirectBufferAddress(buffer);
    jlong capacity = env->GetDirectBufferCapacity(buffer);

    if (!buf || capacity <= 0) {
        LOGE("Invalid direct buffer");
        return -1;
    }

    int ndw = capacity / 4;
    return virgl_renderer_submit_cmd(buf, ctxId, ndw);
}

JNIEXPORT jint JNICALL
Java_com_github_andock_virgl_VirGL_createFence(JNIEnv *env, jclass clazz, jint fenceId,
                                               jint ctxId) {
    return virgl_renderer_create_fence(fenceId, ctxId);
}

// ----------------------------------------------------------------------------
// Data Transfer
// ----------------------------------------------------------------------------

static void get_box_from_jobject(JNIEnv *env, jobject boxObj, struct virgl_box *box) {
    jclass boxClass = env->GetObjectClass(boxObj);
    box->x = env->GetIntField(boxObj, env->GetFieldID(boxClass, "x", "I"));
    box->y = env->GetIntField(boxObj, env->GetFieldID(boxClass, "y", "I"));
    box->z = env->GetIntField(boxObj, env->GetFieldID(boxClass, "z", "I"));
    box->w = env->GetIntField(boxObj, env->GetFieldID(boxClass, "w", "I"));
    box->h = env->GetIntField(boxObj, env->GetFieldID(boxClass, "h", "I"));
    box->d = env->GetIntField(boxObj, env->GetFieldID(boxClass, "d", "I"));
}

JNIEXPORT jint JNICALL
Java_com_github_andock_virgl_VirGL_transferGet(JNIEnv *env, jclass clazz,
                                               jint handle, jint ctxId, jint level, jint stride,
                                               jint layerStride,
                                               jobject boxObj, jlong offset, jobject buffer) {

    void *buf = env->GetDirectBufferAddress(buffer);
    jlong size = env->GetDirectBufferCapacity(buffer);

    if (!buf || size <= 0) {
        LOGE("Invalid direct buffer for transferGet");
        return -1;
    }

    struct virgl_box box;
    get_box_from_jobject(env, boxObj, &box);

    struct iovec iov = {buf, (size_t) size};

    int ret = virgl_renderer_transfer_read_iov(handle, ctxId, level, stride, layerStride,
                                               &box, offset, &iov, 1);
    if (ret != 0) {
        LOGE("virgl_renderer_transfer_read_iov failed: %d", ret);
    }
    return ret;
}

JNIEXPORT jint JNICALL
Java_com_github_andock_virgl_VirGL_transferPut(JNIEnv *env, jclass clazz,
                                               jint handle, jint ctxId, jint level, jint stride,
                                               jint layerStride,
                                               jobject boxObj, jlong offset, jobject buffer) {

    void *buf = env->GetDirectBufferAddress(buffer);
    jlong size = env->GetDirectBufferCapacity(buffer);

    if (!buf || size <= 0) {
        LOGE("Invalid direct buffer for transferPut");
        return -1;
    }

    struct virgl_box box;
    get_box_from_jobject(env, boxObj, &box);

    struct iovec iov = {buf, (size_t) size};

    int ret = virgl_renderer_transfer_write_iov(handle, ctxId, level, stride, layerStride,
                                                &box, offset, &iov, 1);
    if (ret != 0) {
        LOGE("virgl_renderer_transfer_write_iov failed: %d", ret);
    }
    return ret;
}

// ----------------------------------------------------------------------------
// Synchronization
// ----------------------------------------------------------------------------

JNIEXPORT void JNICALL
Java_com_github_andock_virgl_VirGL_poll(JNIEnv *env, jclass clazz) {
    virgl_renderer_poll();
}

} // extern "C"