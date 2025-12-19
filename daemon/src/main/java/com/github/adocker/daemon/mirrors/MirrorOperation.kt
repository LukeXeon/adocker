package com.github.adocker.daemon.mirrors

sealed interface MirrorOperation {
    object Delete : MirrorOperation
    object Check : MirrorOperation
}