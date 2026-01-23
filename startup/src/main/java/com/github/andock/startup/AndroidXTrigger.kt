package com.github.andock.startup

import android.content.Context
import androidx.startup.AppInitializer
import androidx.startup.Initializer
import androidx.startup.R
import com.github.andock.startup.tasks.TaskResult
import com.github.andock.startup.utils.measureTimeMillis

class AndroidXTrigger : Initializer<AndroidXTrigger.Stats> {

    override fun create(context: Context): Stats {
        val tasks: List<TaskResult>
        val ms = measureTimeMillis {
            tasks = context.trigger(context.resources.getString(R.string.androidx_startup))
        }
        return Stats(ms, tasks)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    data class Stats(
        val time: Long,
        val tasks: List<TaskResult>
    )

    companion object {
        fun stats(context: Context): Stats {
            return AppInitializer.getInstance(context)
                .initializeComponent(AndroidXTrigger::class.java)
        }
    }
}