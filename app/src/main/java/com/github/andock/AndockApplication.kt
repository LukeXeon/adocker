package com.github.andock

import android.app.Application
import com.github.andock.startup.trigger
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.DelicateCoroutinesApi
import timber.log.Timber

@OptIn(DelicateCoroutinesApi::class)
@HiltAndroidApp
class AndockApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        trigger().forEach { (key, ms, all) ->
            if (all) {
                Timber.i("trigger: $key, task all: ${ms}ms")
            } else {
                Timber.i("task $key: ${ms}ms")
            }
        }
    }
}
