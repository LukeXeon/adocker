package com.github.andock.gpu.virgl

import com.github.andock.startup.Task


@Task("virGLRendererServer")
fun virGLRendererServer(
    virGLRendererServer: VirGLRendererServer
) {
    virGLRendererServer.start()
}