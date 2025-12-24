package com.github.andock.daemon.registries

sealed interface RegistryOperation {
    object Remove : RegistryOperation
    object Check : RegistryOperation
}