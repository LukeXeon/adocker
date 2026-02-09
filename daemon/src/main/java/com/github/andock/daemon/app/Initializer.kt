package com.github.andock.daemon.app

import android.app.Application
import android.widget.Toast
import com.github.andock.daemon.R
import com.github.andock.startup.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.acra.config.mailSender
import org.acra.config.toast
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import timber.log.Timber

@Task("app")
suspend fun appContext(
    appContext: Application,
    @Suppress("unused")
    @Task("logging")
    logging: Unit,
    @Suppress("unused")
    @Task("reporter")
    reporter: Unit
): Application {
    withContext(Dispatchers.Default) {
        listOf(
            appContext.containersDir,
            appContext.layersDir,
        ).forEach { dir ->
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }
        Timber.d("AppContext initialized")
    }
    return appContext
}

@Task("reporter")
suspend fun crashReporter(
    @Suppress("unused")
    @Task("logging")
    logging: Unit,
    application: Application
) {
    withContext(Dispatchers.Default) {
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
            toast {
                //required
                text = "Crash send to developer email"
                //defaults to Toast.LENGTH_LONG
                length = Toast.LENGTH_LONG
            }
        }
    }
}