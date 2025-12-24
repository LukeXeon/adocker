package com.github.andock.daemon.os

sealed interface ProcessLocator {
    fun getPid(process: Process): Int

    object Parser : ProcessLocator {
        private val regex = Regex("pid=(\\d+)")
        override fun getPid(process: Process): Int {
            return regex.find(process.toString())?.groups?.get(1)?.value?.toIntOrNull() ?: 0
        }
    }

    abstract class Reflection : ProcessLocator {

        override fun getPid(process: Process): Int {
            val pid = getField(process)
            if (pid != 0) {
                return pid
            }
            return Parser.getPid(process)
        }

        abstract fun getField(process: Process): Int
    }

}