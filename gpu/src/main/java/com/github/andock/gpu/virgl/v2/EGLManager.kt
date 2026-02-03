package com.github.andock.gpu.virgl.v2

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.view.Surface
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EGL Manager
 *
 * Manages EGL display and contexts for VirGL rendering.
 */
@Singleton
class EGLManager @Inject constructor() {
    private var display: EGLDisplay? = EGL14.EGL_NO_DISPLAY
    private var config: EGLConfig? = null
    private var initialized = false

    /**
     * Initialize EGL and choose config
     */
    fun initialize(): Boolean {
        if (initialized) {
            return true
        }

        // Get default display
        display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (display === EGL14.EGL_NO_DISPLAY) {
            Timber.e("eglGetDisplay failed")
            return false
        }

        // Initialize EGL
        val version = IntArray(2)
        if (!EGL14.eglInitialize(display, version, 0, version, 1)) {
            display = EGL14.EGL_NO_DISPLAY
            Timber.e("eglInitialize failed")
            return false
        }
        Timber.i("EGL initialized: version ${version[0]}.${version[1]}")

        // Choose config
        val attrs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 24,
            EGL14.EGL_STENCIL_SIZE, 8,
            EGL14.EGL_NONE
        )

        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfig = IntArray(1)
        if (!EGL14.eglChooseConfig(
                display,
                attrs,
                0,
                configs,
                0,
                1,
                numConfig,
                0
            ) || numConfig[0] == 0
        ) {
            display = EGL14.EGL_NO_DISPLAY
            Timber.e("eglChooseConfig failed: 0x${EGL14.eglGetError().toHexString()}")
            return false
        }

        config = configs[0]
        initialized = true
        return true
    }

    /**
     * Create an OpenGL ES 3.0 context
     */
    fun createContext(): EGLContext? {
        if (!initialized && !initialize()) {
            return null
        }

        val ctxAttrs = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
            EGL14.EGL_NONE
        )

        val ctx = EGL14.eglCreateContext(
            display,
            config,
            EGL14.EGL_NO_CONTEXT,
            ctxAttrs,
            0
        )
        if (ctx == EGL14.EGL_NO_CONTEXT) {
            Timber.e("eglCreateContext failed: 0x${EGL14.eglGetError().toHexString()}")
            return null
        }

        return ctx
    }

    /**
     * Create a window surface from Android Surface
     */
    fun createWindowSurface(surface: Surface): EGLSurface? {
        if (!initialized && !initialize()) {
            return null
        }

        val eglSurface = EGL14.eglCreateWindowSurface(
            display,
            config,
            surface,
            intArrayOf(EGL14.EGL_NONE),
            0
        )
        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            Timber.e("eglCreateWindowSurface failed: 0x${EGL14.eglGetError().toHexString()}")
            return null
        }

        Timber.d("Window surface created: $eglSurface")
        return eglSurface
    }

    /**
     * Destroy an EGL context
     */
    fun destroyContext(context: EGLContext?) {
        if (context != null && context != EGL14.EGL_NO_CONTEXT) {
            EGL14.eglDestroyContext(display, context)
        }
    }

    /**
     * Destroy an EGL surface
     */
    fun destroySurface(surface: EGLSurface?) {
        if (surface != null && surface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroySurface(display, surface)
        }
    }

    /**
     * Make the specified context and surface current
     */
    fun makeCurrent(context: EGLContext?, surface: EGLSurface?): Boolean {
        if (!EGL14.eglMakeCurrent(display, surface, surface, context)) {
            Timber.e("eglMakeCurrent failed: 0x${EGL14.eglGetError().toHexString()}")
            return false
        }
        return true
    }

    /**
     * Get the EGL display
     */
    fun getDisplay(): EGLDisplay? = display

    /**
     * Cleanup EGL resources
     */
    fun terminate() {
        if (display != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglTerminate(display)
            display = EGL14.EGL_NO_DISPLAY
        }
        initialized = false
    }
}