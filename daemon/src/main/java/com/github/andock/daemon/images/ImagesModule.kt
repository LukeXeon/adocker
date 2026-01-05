package com.github.andock.daemon.images

import com.github.andock.daemon.utils.SuspendLazy
import com.github.andock.daemon.utils.suspendLazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ImagesModule {

    @Provides
    @Singleton
    @Named("images")
    fun initializer(
        @Named("app")
        app: SuspendLazy<Unit>,
        imageManager: ImageManager,
    ) = suspendLazy {
        app.getValue()
        imageManager.deleteUnreferencedLayers()
    }

    @Provides
    @IntoMap
    @StringKey("images")
    fun initializerToMap(
        @Named("images") task: SuspendLazy<Unit>
    ): SuspendLazy<*> = task
}