package com.github.andock.proot

object PRoot {

    init {
        System.loadLibrary("proot_ext")
    }

    @JvmStatic
    external fun getVersion(): String
}