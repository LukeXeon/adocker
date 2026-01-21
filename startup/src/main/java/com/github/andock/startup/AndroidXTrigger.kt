package com.github.andock.startup

import android.content.Context
import androidx.startup.Initializer
import androidx.startup.R

class AndroidXTrigger : Initializer<List<TaskResult>> {

    override fun create(context: Context): List<TaskResult> {
        results = context.trigger(context.resources.getString(R.string.androidx_startup))
        return results
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    companion object {
        @Volatile
        var results: List<TaskResult> = emptyList()
            private set
    }
}