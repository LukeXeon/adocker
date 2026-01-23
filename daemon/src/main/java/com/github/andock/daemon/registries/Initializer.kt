package com.github.andock.daemon.registries

import com.github.andock.startup.tasks.Task


@Task("checkAllRegistries")
suspend fun checkAllRegistries(
    registryManager: RegistryManager,
    @Suppress("unused")
    @Task("logging")
    logging: Unit,
    @Suppress("unused")
    @Task("reporter")
    reporter: Unit,
) {
    registryManager.checkAll()
}