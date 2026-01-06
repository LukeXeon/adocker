package com.github.andock.daemon.app

import android.app.Application
import com.github.andock.daemon.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.acra.config.mailSender
import org.acra.data.StringFormat
import org.acra.ktx.initAcra

@AppTask("app")
fun appContext(
    appContext: AppContext,
    @Suppress("unused")
    @AppTask("logging")
    logging: Unit,
    @Suppress("unused")
    @AppTask("reporter")
    reporter: Unit
): AppContext {
    appContext.initializeDirs()
    return appContext
}

@AppTask("reporter")
suspend fun crashReporter(
    @Suppress("unused")
    @AppTask("logging")
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