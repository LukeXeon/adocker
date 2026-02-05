/**
 * VirGL JNI Implementation
 *
 * Provides JNI bindings for virglrenderer with Android EGL integration.
 * Implements vtest protocol server with epoll multi-client support.
 */

#include <jni.h>
#include <android/log.h>
#include <android/native_window_jni.h>
#include <memory>
#include <string>

#include "vtest_epoll_server.h"
#include "vtest_client.h"
#include "virgl_egl_manager.h"

#define LOG_TAG "VirGL-JNI"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

// Global server instance
static std::unique_ptr<EpollServer> g_server;

/**
 * JNI: Start vtest server
 *
 * Signature: native fun nativeStartServer(socketPath: String): Int
 */
extern "C"
JNIEXPORT jint JNICALL
Java_com_github_andock_gpu_virgl_VirGLServer_nativeStartServer(
        JNIEnv* env,
        jobject /* this */,
        jstring socketPath) {

    LOGI("nativeStartServer() called");

    if (!socketPath) {
        LOGE("Socket path is null");
        return -1;
    }

    // Get socket path string
    const char* path = env->GetStringUTFChars(socketPath, nullptr);
    if (!path) {
        LOGE("Failed to get socket path string");
        return -1;
    }

    LOGI("Socket path: %s", path);

    // Create server instance
    g_server = std::make_unique<EpollServer>();

    // Initialize server
    if (!g_server->init(path)) {
        LOGE("Failed to initialize server");
        env->ReleaseStringUTFChars(socketPath, path);
        g_server.reset();
        return -1;
    }

    env->ReleaseStringUTFChars(socketPath, path);

    // Run server (blocks until stopped)
    LOGI("Starting server event loop");
    g_server->run();

    LOGI("Server event loop exited");
    g_server.reset();

    return 0;
}

/**
 * JNI: Stop vtest server
 *
 * Signature: native fun nativeStopServer()
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_github_andock_gpu_virgl_VirGLServer_nativeStopServer(
        JNIEnv* env,
        jobject /* this */) {

    LOGI("nativeStopServer() called");

    if (g_server) {
        g_server->stop();
    }
}
