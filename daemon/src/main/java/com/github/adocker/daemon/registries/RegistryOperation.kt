package com.github.adocker.daemon.registries

sealed interface RegistryOperation {
    object Remove : RegistryOperation
    object Check : RegistryOperation
}