#ifndef VIRGL_EGL_MANAGER_H
#define VIRGL_EGL_MANAGER_H

#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <android/native_window.h>

/**
 * EGL Manager - Singleton class for managing EGL display, contexts, and surfaces
 *
 * Responsibilities:
 * - Initialize EGL display and choose appropriate config
 * - Create/destroy EGL contexts (with optional sharing)
 * - Create/destroy EGL surfaces (both window and pbuffer)
 * - Make contexts current
 *
 * Thread Safety:
 * - Initialization is thread-safe (called once)
 * - Other methods should be called from the same thread (epoll thread)
 */
class EGLManager {
public:
    /**
     * Get singleton instance
     */
    static EGLManager* instance();

    /**
     * Initialize EGL display and choose config
     * @return true if successful
     */
    bool initialize();

    /**
     * Create an EGL context
     * @param shared Optional shared context for resource sharing (EGL_NO_CONTEXT for none)
     * @return EGLContext or EGL_NO_CONTEXT on failure
     */
    EGLContext createContext(EGLContext shared = EGL_NO_CONTEXT);

    /**
     * Create a window surface from ANativeWindow
     * @param window Native window from SurfaceView
     * @return EGLSurface or EGL_NO_SURFACE on failure
     */
    EGLSurface createWindowSurface(ANativeWindow* window);

    /**
     * Create a pbuffer surface for offscreen rendering
     * @param width Surface width
     * @param height Surface height
     * @return EGLSurface or EGL_NO_SURFACE on failure
     */
    EGLSurface createPbufferSurface(int width = 1280, int height = 720);

    /**
     * Destroy an EGL context
     * @param context Context to destroy
     */
    void destroyContext(EGLContext context);

    /**
     * Destroy an EGL surface
     * @param surface Surface to destroy
     */
    void destroySurface(EGLSurface surface);

    /**
     * Make a context current
     * @param context Context to activate (EGL_NO_CONTEXT to unbind)
     * @param surface Surface to bind
     * @return true if successful
     */
    bool makeCurrent(EGLContext context, EGLSurface surface);

    /**
     * Get the EGL display
     */
    EGLDisplay getDisplay() const { return display_; }

    /**
     * Get the EGL config
     */
    EGLConfig getConfig() const { return config_; }

    /**
     * Check if initialized
     */
    bool isInitialized() const { return initialized_; }

    /**
     * Terminate EGL (cleanup)
     */
    void terminate();

private:
    EGLManager() = default;
    ~EGLManager();

    // Non-copyable
    EGLManager(const EGLManager&) = delete;
    EGLManager& operator=(const EGLManager&) = delete;

    EGLDisplay display_ = EGL_NO_DISPLAY;
    EGLConfig config_ = nullptr;
    bool initialized_ = false;
};

#endif // VIRGL_EGL_MANAGER_H
