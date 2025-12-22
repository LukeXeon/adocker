package com.github.adocker.daemon.registries

sealed interface RegistryOperation {
    object Delete : RegistryOperation
    object Check : RegistryOperation
}