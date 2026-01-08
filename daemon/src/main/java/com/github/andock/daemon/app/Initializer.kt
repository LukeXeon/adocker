package com.github.andock.daemon.app

import android.app.Application
import com.github.andock.daemon.R
import com.github.andock.startup.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.acra.config.mailSender
import org.acra.data.StringFormat
import org.acra.ktx.initAcra

@Task("app")
fun appContext(
    appContext: AppContext,
    @Suppress("unused")
    @Task("logging")
    logging: Unit,
    @Suppress("unused")
    @Task("reporter")
    reporter: Unit
): AppContext {
    appContext.initializeDirs()
    return appContext
}

@Task("reporter")
suspend fun crashReporter(
    @Suppress("unused")
    @Task("logging")
    logging: Unit,
    application: Application
) {
    withContext(Dispatchers.IO) {
        application.initAcra {
            buildConfigClass = sequenceOf(
                application.packageName,
                "${application.packageName}.daemon"
            ).mapNotNull {
                runCatching {
                    Class.forName("${it}.BuildConfig")
                }.getOrNull()
            }.firstOrNull() ?: throw AssertionError("No found BuildConfig class")
            reportFormat = StringFormat.JSON
            mailSender {
                mailTo = application.getString(R.string.qq_email)
            }
        }
    }
}