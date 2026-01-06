package com.github.andock.daemon.registries

import com.github.andock.daemon.app.AppTask


@AppTask("registry")
suspend fun registry(
    registryManager: RegistryManager,
    @Suppress("unused")
    @AppTask("logging")
    logging: Unit,
    @Suppress("unused")
    @AppTask("reporter")
    reporter: Unit
) {
    registryManager.checkAll()
}