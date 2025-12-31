package com.github.andock.daemon.app

import android.app.Application
import com.github.andock.daemon.utils.SuspendLazy
import com.github.andock.daemon.utils.suspendLazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun json(): Json {
        return Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
            prettyPrint = true
        }
    }

    @Provides
    @Singleton
    fun scope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, e ->
            if (e !is CancellationException) {
                Timber.e(e)
            }
        })
    }

    @Provides
    @Singleton
    fun httpClient(
        json: Json,
        appContext: AppContext,
        app: Application,
    ): HttpClient {
        return HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(json)
            }
            install(Logging) {
                level = LogLevel.HEADERS
            }
            install(HttpTimeout) {
                requestTimeoutMillis = AppContext.NETWORK_TIMEOUT
                connectTimeoutMillis = AppContext.NETWORK_TIMEOUT
                socketTimeoutMillis = AppContext.DOWNLOAD_TIMEOUT
            }
            defaultRequest {
                header(
                    HttpHeaders.UserAgent,
                    "${app.resources.getString(app.applicationInfo.labelRes)}/${appContext.packageInfo.versionName}"
                )
            }
        }
    }

    @Provides
    @Singleton
    @Named("app")
    fun initializer(
        appContext: AppContext,
        @Named("logging")
        logging: SuspendLazy<Unit>,
        @Named("reporter")
        reporter: SuspendLazy<Unit>
    ) = suspendLazy {
        logging.getValue()
        reporter.getValue()
        withContext(Dispatchers.IO) {
            appContext.logDir.deleteRecursively()
            // Create directories on initialization
            listOf(
                appContext.containersDir,
                appContext.layersDir,
                appContext.logDir,
            ).forEach { dir ->
                if (!dir.exists()) {
                    dir.mkdirs()
                }
            }
            Timber.d("AppConfig initialized: baseDir=${appContext.baseDir.absolutePath}")
        }
    }

    @Provides
    @IntoMap
    @StringKey("app")
    fun initializerToMap(
        @Named("app") task: SuspendLazy<Unit>
    ): SuspendLazy<*> = task
}
