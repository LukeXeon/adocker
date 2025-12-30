package com.github.andock.daemon.os

sealed interface ProcessLocator {
    fun locate(process: Process): Int

    object Parser : ProcessLocator {
        private val regex = Regex("pid=(\\d+)")
        override fun locate(process: Process): Int {
            return regex.find(process.toString())?.groups?.get(1)?.value?.toIntOrNull() ?: 0
        }
    }

    abstract class Reflection : ProcessLocator {

        override fun locate(process: Process): Int {
            val pid = getField(process)
            if (pid != 0) {
                return pid
            }
            return Parser.locate(process)
        }

        abstract fun getField(process: Process): Int
    }

}