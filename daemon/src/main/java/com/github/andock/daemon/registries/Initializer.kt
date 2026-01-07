package com.github.andock.daemon.registries

import com.github.andock.daemon.app.AppTask
import com.github.andock.daemon.database.AppDatabase


@AppTask("checkAllRegistries")
suspend fun checkAllRegistries(
    registryManager: RegistryManager,
    @Suppress("unused")
    @AppTask("logging")
    logging: Unit,
    @Suppress("unused")
    @AppTask("reporter")
    reporter: Unit,
) {
    registryManager.checkAll()
}