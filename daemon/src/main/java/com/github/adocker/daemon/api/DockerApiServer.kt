package com.github.adocker.daemon.api

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DockerApiServer @Inject constructor() : HttpHandler {
    override fun invoke(p1: Request): Response {
        TODO("Not yet implemented")
    }
}