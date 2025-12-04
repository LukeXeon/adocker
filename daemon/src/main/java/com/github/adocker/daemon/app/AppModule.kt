package com.github.adocker.daemon.app

import android.annotation.SuppressLint
import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import timber.log.Timber
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
        return CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { context, e ->
            if (e !is CancellationException) {
                Timber.e(e)
            }
        })
    }
}
