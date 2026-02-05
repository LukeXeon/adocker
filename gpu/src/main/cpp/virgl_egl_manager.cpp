#include "virgl_egl_manager.h"
#include <android/log.h>

#define LOG_TAG "VirGL-EGL"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

EGLManager* EGLManager::instance() {
    static EGLManager instance;
    return &instance;
}

bool EGLManager::initialize() {
    if (initialized_) {
        LOGD("EGL already initialized");
        return true;
    }

    // 1. Get default EGL display
    display_ = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (display_ == EGL_NO_DISPLAY) {
        LOGE("Failed to get EGL display: 0x%x", eglGetError());
        return false;
    }

    // 2. Initialize EGL
    EGLint major, minor;
    if (!eglInitialize(display_, &major, &minor)) {
        LOGE("Failed to initialize EGL: 0x%x", eglGetError());
        display_ = EGL_NO_DISPLAY;
        return false;
    }

    LOGI("EGL initialized: version %d.%d", major, minor);

    // 3. Choose EGL config
    // Requirements:
    // - OpenGL ES 3.0 support
    // - Support both window and pbuffer surfaces
    // - 8-bit RGBA
    // - 24-bit depth buffer
    // - 8-bit stencil buffer
    EGLint attribs[] = {
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
        EGL_SURFACE_TYPE, EGL_PBUFFER_BIT | EGL_WINDOW_BIT,
        EGL_RED_SIZE, 8,
        EGL_GREEN_SIZE, 8,
        EGL_BLUE_SIZE, 8,
        EGL_ALPHA_SIZE, 8,
        EGL_DEPTH_SIZE, 24,
        EGL_STENCIL_SIZE, 8,
        EGL_NONE
    };

    EGLint num_configs;
    if (!eglChooseConfig(display_, attribs, &config_, 1, &num_configs) || num_configs == 0) {
        LOGE("Failed to choose EGL config: 0x%x", eglGetError());
        eglTerminate(display_);
        display_ = EGL_NO_DISPLAY;
        return false;
    }

    LOGI("EGL config chosen (num_configs=%d)", num_configs);

    // Log config details
    EGLint red, green, blue, alpha, depth, stencil;
    eglGetConfigAttrib(display_, config_, EGL_RED_SIZE, &red);
    eglGetConfigAttrib(display_, config_, EGL_GREEN_SIZE, &green);
    eglGetConfigAttrib(display_, config_, EGL_BLUE_SIZE, &blue);
    eglGetConfigAttrib(display_, config_, EGL_ALPHA_SIZE, &alpha);
    eglGetConfigAttrib(display_, config_, EGL_DEPTH_SIZE, &depth);
    eglGetConfigAttrib(display_, config_, EGL_STENCIL_SIZE, &stencil);
    LOGI("EGL config: R%dG%dB%dA%d D%dS%d", red, green, blue, alpha, depth, stencil);

    initialized_ = true;
    return true;
}

EGLContext EGLManager::createContext(EGLContext shared) {
    if (!initialized_) {
        LOGE("EGL not initialized");
        return EGL_NO_CONTEXT;
    }

    // Create OpenGL ES 3.0 context
    EGLint ctx_attribs[] = {
        EGL_CONTEXT_CLIENT_VERSION, 3,
        EGL_NONE
    };

    EGLContext ctx = eglCreateContext(display_, config_, shared, ctx_attribs);
    if (ctx == EGL_NO_CONTEXT) {
        LOGE("Failed to create EGL context: 0x%x", eglGetError());
        return EGL_NO_CONTEXT;
    }

    if (shared != EGL_NO_CONTEXT) {
        LOGD("Created EGL context %p (shared with %p)", ctx, shared);
    } else {
        LOGD("Created EGL context %p (no sharing)", ctx);
    }

    return ctx;
}

EGLSurface EGLManager::createWindowSurface(ANativeWindow* window) {
    if (!initialized_) {
        LOGE("EGL not initialized");
        return EGL_NO_SURFACE;
    }

    if (!window) {
        LOGE("Invalid native window");
        return EGL_NO_SURFACE;
    }

    EGLSurface surface = eglCreateWindowSurface(display_, config_, window, nullptr);
    if (surface == EGL_NO_SURFACE) {
        LOGE("Failed to create window surface: 0x%x", eglGetError());
        return EGL_NO_SURFACE;
    }

    // Get surface dimensions
    EGLint width, height;
    eglQuerySurface(display_, surface, EGL_WIDTH, &width);
    eglQuerySurface(display_, surface, EGL_HEIGHT, &height);

    LOGD("Created window surface %p (%dx%d)", surface, width, height);

    return surface;
}

EGLSurface EGLManager::createPbufferSurface(int width, int height) {
    if (!initialized_) {
        LOGE("EGL not initialized");
        return EGL_NO_SURFACE;
    }

    EGLint pbuffer_attribs[] = {
        EGL_WIDTH, width,
        EGL_HEIGHT, height,
        EGL_NONE
    };

    EGLSurface surface = eglCreatePbufferSurface(display_, config_, pbuffer_attribs);
    if (surface == EGL_NO_SURFACE) {
        LOGE("Failed to create pbuffer surface: 0x%x", eglGetError());
        return EGL_NO_SURFACE;
    }

    LOGD("Created pbuffer surface %p (%dx%d)", surface, width, height);

    return surface;
}

void EGLManager::destroyContext(EGLContext context) {
    if (context == EGL_NO_CONTEXT) {
        return;
    }

    // Ensure context is not current before destroying
    EGLContext current_ctx = eglGetCurrentContext();
    if (current_ctx == context) {
        LOGW("Destroying current context, unbinding first");
        eglMakeCurrent(display_, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    }

    if (!eglDestroyContext(display_, context)) {
        LOGE("Failed to destroy context %p: 0x%x", context, eglGetError());
    } else {
        LOGD("Destroyed EGL context %p", context);
    }
}

void EGLManager::destroySurface(EGLSurface surface) {
    if (surface == EGL_NO_SURFACE) {
        return;
    }

    // Ensure surface is not current before destroying
    EGLSurface current_surf = eglGetCurrentSurface(EGL_DRAW);
    if (current_surf == surface) {
        LOGW("Destroying current surface, unbinding first");
        eglMakeCurrent(display_, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    }

    if (!eglDestroySurface(display_, surface)) {
        LOGE("Failed to destroy surface %p: 0x%x", surface, eglGetError());
    } else {
        LOGD("Destroyed EGL surface %p", surface);
    }
}

bool EGLManager::makeCurrent(EGLContext context, EGLSurface surface) {
    if (!initialized_) {
        LOGE("EGL not initialized");
        return false;
    }

    // Special case: unbind context
    if (context == EGL_NO_CONTEXT) {
        if (!eglMakeCurrent(display_, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT)) {
            LOGE("Failed to unbind EGL context: 0x%x", eglGetError());
            return false;
        }
        LOGD("Unbound EGL context");
        return true;
    }

    // Make context current
    if (!eglMakeCurrent(display_, surface, surface, context)) {
        LOGE("Failed to make context %p current: 0x%x", context, eglGetError());
        return false;
    }

    return true;
}

void EGLManager::terminate() {
    if (!initialized_) {
        return;
    }

    LOGI("Terminating EGL");

    // Unbind current context
    eglMakeCurrent(display_, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);

    // Terminate display
    if (display_ != EGL_NO_DISPLAY) {
        eglTerminate(display_);
        display_ = EGL_NO_DISPLAY;
    }

    initialized_ = false;
}

EGLManager::~EGLManager() {
    terminate();
}
