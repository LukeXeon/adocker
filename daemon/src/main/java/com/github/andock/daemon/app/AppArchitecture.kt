package com.github.andock.daemon.app

import android.os.Build

object AppArchitecture {
    private fun mapAbi(supportedAbis: Array<String>): String {
        return when (supportedAbis.firstOrNull()) {
            "arm64-v8a" -> "arm64"
            "armeabi-v7a" -> "arm"
            "x86_64" -> "amd64"
            "x86" -> "386"
            else -> ""
        }
    }

    val DEFAULT = mapAbi(Build.SUPPORTED_ABIS)

    val DEFAULT_64_BIT = mapAbi(Build.SUPPORTED_64_BIT_ABIS)

    val DEFAULT_32_BIT = mapAbi(Build.SUPPORTED_32_BIT_ABIS)

    const val OS = "linux"
}