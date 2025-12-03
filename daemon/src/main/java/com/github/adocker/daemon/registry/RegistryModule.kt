package com.github.adocker.daemon.registry

import com.github.adocker.daemon.app.AppConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RegistryModule {

    @Provides
    @Singleton
    fun httpClient(
        json: Json,
        appConfig: AppConfig
    ): HttpClient {
        return HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(json)
            }
            install(Logging) {
                level = LogLevel.HEADERS
            }
            install(HttpTimeout) {
                requestTimeoutMillis = AppConfig.NETWORK_TIMEOUT
                connectTimeoutMillis = AppConfig.NETWORK_TIMEOUT
                socketTimeoutMillis = AppConfig.DOWNLOAD_TIMEOUT
            }
            defaultRequest {
                header(
                    HttpHeaders.UserAgent,
                    "${requireNotNull(appConfig.packageInfo.applicationInfo).name}/${appConfig.packageInfo.versionName}"
                )
            }
        }
    }

}