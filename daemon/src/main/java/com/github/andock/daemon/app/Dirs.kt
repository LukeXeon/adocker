package com.github.andock.daemon.app

import android.content.Context
import java.io.File


// Directories
val Context.containersDir
    get() = File(dataDir, DIR_CONTAINERS)
val Context.layersDir
    get() = File(dataDir, DIR_LAYERS)

val Context.socketFile
    get() = File(cacheDir, DOCKER_SOCK)

private const val DOCKER_SOCK = "docker.sock"
private const val DIR_CONTAINERS = "containers"
private const val DIR_LAYERS = "layers"