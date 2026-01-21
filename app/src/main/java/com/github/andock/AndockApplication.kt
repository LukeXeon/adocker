package com.github.andock

import android.app.Application
import com.github.andock.startup.AndroidXTrigger
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.DelicateCoroutinesApi
import timber.log.Timber

@OptIn(DelicateCoroutinesApi::class)
@HiltAndroidApp
class AndockApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AndroidXTrigger.results.forEach { task ->
            Timber.i("${task.name}, phaseTime: ${task.phaseTime}ms, totalTime: ${task.totalTime}")
        }
    }
}
