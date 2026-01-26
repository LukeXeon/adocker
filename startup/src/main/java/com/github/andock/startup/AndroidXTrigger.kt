package com.github.andock.startup

import android.content.Context
import androidx.startup.Initializer
import androidx.startup.R


internal class AndroidXTrigger : Initializer<Stats> {

    override fun create(context: Context): Stats {
        return context.trigger(context.resources.getString(R.string.androidx_startup))
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}