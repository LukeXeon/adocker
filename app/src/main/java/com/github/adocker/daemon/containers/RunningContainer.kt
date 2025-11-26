package com.github.adocker.daemon.containers

import com.github.adocker.daemon.utils.isActive

class RunningContainer {
    private val lock = Any()
    private val processes = ArrayList<RunningProcess>()

    val isActive: Boolean
        get() {
            synchronized(lock) {
                return processes.any { it.process.isActive }
            }
        }

    fun killAll() {
        synchronized(lock) {
            processes.forEach {
                it.process.destroy()
            }
        }
    }
}