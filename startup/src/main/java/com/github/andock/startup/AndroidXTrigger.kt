package com.github.andock.startup

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.startup.Initializer
import androidx.startup.R
import com.github.andock.startup.tasks.TaskResult
import com.github.andock.startup.utils.measureTimeMillis


@RestrictTo(RestrictTo.Scope.LIBRARY)
class AndroidXTrigger : Initializer<Stats> {

    override fun create(context: Context): Stats {
        val tasks: List<TaskResult>
        val ms = measureTimeMillis {
            tasks = context.trigger(context.resources.getString(R.string.androidx_startup))
        }
        return Stats(ms, tasks)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}