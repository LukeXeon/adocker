package com.github.andock.daemon.debug

import android.app.Application
import com.github.andock.daemon.R
import com.github.andock.daemon.utils.SuspendLazy
import com.github.andock.daemon.utils.suspendLazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import org.acra.config.mailSender
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CrashReportModule {
    @Provides
    @Singleton
    @Named("reporter")
    fun initializer(
        application: Application,
        @Named("logging")
        logging: SuspendLazy<Unit>
    ) = suspendLazy {
        logging.getValue()
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

    @Provides
    @IntoMap
    @StringKey("reporter")
    fun initializerToMap(
        @Named("reporter") task: SuspendLazy<Unit>
    ): SuspendLazy<*> = task
}