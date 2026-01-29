package com.github.andock.gpu

class NativeLib {

    /**
     * A native method that is implemented by the 'gpu' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'gpu' library on application startup.
        init {
            System.loadLibrary("gpu")
        }
    }
}