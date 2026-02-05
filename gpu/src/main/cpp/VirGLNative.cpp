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

/**
 * JNI: Attach surface to client for rendering
 *
 * Signature: native fun nativeAttachSurface(clientFd: Int, surface: Surface)
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_github_andock_gpu_demo_VirGLSurfaceView_nativeAttachSurface(
        JNIEnv* env,
        jobject /* this */,
        jint clientFd,
        jobject surface) {

    LOGI("nativeAttachSurface(fd=%d)", clientFd);

    if (!g_server) {
        LOGE("Server not running");
        return;
    }

    if (!surface) {
        LOGE("Surface is null");
        return;
    }

    // Get native window from Surface
    ANativeWindow* window = ANativeWindow_fromSurface(env, surface);
    if (!window) {
        LOGE("Failed to get ANativeWindow from Surface");
        return;
    }

    // Get client
    VTestClient* client = g_server->getClient(clientFd);
    if (!client) {
        LOGE("Client %d not found", clientFd);
        ANativeWindow_release(window);
        return;
    }

    // Create window surface
    EGLSurface egl_surface = EGLManager::instance()->createWindowSurface(window);
    if (egl_surface == EGL_NO_SURFACE) {
        LOGE("Failed to create window surface");
        ANativeWindow_release(window);
        return;
    }

    // Attach surface to client
    client->setEGLSurface(egl_surface, window);

    // Note: window refcount is managed by client, will be released in destructor
    LOGI("Surface attached to client %d", clientFd);
}

/**
 * JNI: Detach surface from client
 *
 * Signature: native fun nativeDetachSurface(clientFd: Int)
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_github_andock_gpu_demo_VirGLSurfaceView_nativeDetachSurface(
        JNIEnv* env,
        jobject /* this */,
        jint clientFd) {

    LOGI("nativeDetachSurface(fd=%d)", clientFd);

    if (!g_server) {
        LOGE("Server not running");
        return;
    }

    VTestClient* client = g_server->getClient(clientFd);
    if (!client) {
        LOGE("Client %d not found", clientFd);
        return;
    }

    // Create a new pbuffer surface (detach from window)
    EGLSurface pbuffer = EGLManager::instance()->createPbufferSurface(1, 1);
    if (pbuffer != EGL_NO_SURFACE) {
        client->setEGLSurface(pbuffer, nullptr);
    }

    LOGI("Surface detached from client %d", clientFd);
}
