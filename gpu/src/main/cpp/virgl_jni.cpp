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
// JNI Exports
// ============================================================================

extern "C" {

} // extern "C"