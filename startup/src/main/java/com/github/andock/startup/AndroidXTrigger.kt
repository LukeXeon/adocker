package com.github.andock.startup

import android.content.Context
import androidx.startup.AppInitializer
import androidx.startup.Initializer
import androidx.startup.R

class AndroidXTrigger : Initializer<List<TaskResult>> {

    override fun create(context: Context): List<TaskResult> {
        return context.trigger(context.resources.getString(R.string.androidx_startup))
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    companion object {
        fun getResults(context: Context): List<TaskResult> {
            return AppInitializer.getInstance(context).initializeComponent(
                AndroidXTrigger::class.java
            )
        }
    }
}