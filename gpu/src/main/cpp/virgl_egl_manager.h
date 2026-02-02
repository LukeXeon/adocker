/**
 * VirGL EGL Manager
 *
 * Manages EGL display and contexts for Android
 */

#ifndef VIRGL_EGL_MANAGER_H
#define VIRGL_EGL_MANAGER_H

#include <EGL/egl.h>
#include <EGL/eglext.h>

/**
 * EGL Manager Singleton
 * Manages the global EGLDisplay and provides context creation
 */
class EGLManager {
public:
    static EGLManager* instance();

    // Initialize EGL (called once)
    bool initialize();

    // Create an EGL context
    EGLContext createContext();

    // Create a pbuffer surface
    EGLSurface createPbufferSurface(int width = 1, int height = 1);

    // Create a window surface from ANativeWindow
    EGLSurface createWindowSurface(ANativeWindow* window);

    // Destroy context
    void destroyContext(EGLContext context);

    // Destroy surface
    void destroySurface(EGLSurface surface);

    // Make context current
    bool makeCurrent(EGLContext context, EGLSurface surface);

    // Get display
    EGLDisplay getDisplay() const { return display_; }

private:
    EGLManager();
    ~EGLManager();

    EGLDisplay display_ = EGL_NO_DISPLAY;
    EGLConfig config_ = nullptr;
    bool initialized_ = false;
};

#endif // VIRGL_EGL_MANAGER_H
