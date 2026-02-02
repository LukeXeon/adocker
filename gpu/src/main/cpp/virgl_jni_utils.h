/**
 * VirGL JNI Utilities
 *
 * Thread-safe JNI helper functions
 */

#ifndef VIRGL_JNI_UTILS_H
#define VIRGL_JNI_UTILS_H

#include <jni.h>
#include <android/log.h>

// Logging macros
#define LOG_TAG "VirGL_JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

// Global JavaVM (set in JNI_OnLoad)
extern JavaVM* g_jvm;

/**
 * Get JNIEnv for the current thread
 * Automatically attaches the thread if needed
 */
JNIEnv* getJNIEnv();

/**
 * Throw a Java exception
 */
void throwJavaException(JNIEnv* env, const char* className, const char* message);

/**
 * Throw IOException
 */
inline void throwIOException(JNIEnv* env, const char* message) {
    throwJavaException(env, "java/io/IOException", message);
}

/**
 * Throw RuntimeException
 */
inline void throwRuntimeException(JNIEnv* env, const char* message) {
    throwJavaException(env, "java/lang/RuntimeException", message);
}

#endif // VIRGL_JNI_UTILS_H
