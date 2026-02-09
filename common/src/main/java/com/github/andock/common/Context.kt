package com.github.andock.common

import android.app.ActivityThread
import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.view.inspector.WindowInspector
import java.io.File


val application by lazy(LazyThreadSafetyMode.PUBLICATION) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        WindowInspector.getGlobalWindowViews().firstOrNull()
            ?.context
            ?.applicationContext as? Application
    } else {
        null
    } ?: ActivityThread.currentApplication()
}

val Context.isDebuggable
    get() = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

val Context.packageInfo
    get() = requireNotNull(
        packageManager.getPackageInfo(packageName, 0)
    )

val Context.nativeLibDir
    get() = File(requireNotNull(applicationInfo.nativeLibraryDir))