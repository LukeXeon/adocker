package com.github.andock

import android.app.Application
import com.github.andock.startup.stats
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class AndockApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val stats = stats
        stats.tasks.forEach { task ->
            Timber.i("task: ${task.name}, phaseTime: ${task.phaseTime}ms")
        }
        Timber.i("trigger: ${stats.name}, totalTime: ${stats.totalTime}ms")
    }
}
