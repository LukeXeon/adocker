package com.github.andock.ui.screens.main

import androidx.collection.MutableObjectIntMap
import androidx.collection.ObjectIntMap
import com.github.andock.ui.screens.containers.ContainersRoute
import com.github.andock.ui.screens.home.HomeRoute
import com.github.andock.ui.screens.images.ImagesRoute
import com.github.andock.ui.screens.search.SearchRoute
import com.github.andock.ui.screens.settings.SettingsRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.reflect.KClass

@Module
@InstallIn(SingletonComponent::class)
object MainModule {

    @Provides
    @Singleton
    fun order(): ObjectIntMap<KClass<*>> = arrayOf(
        HomeRoute::class,
        SearchRoute::class,
        ContainersRoute::class,
        ImagesRoute::class,
        SettingsRoute::class
    ).asSequence().mapIndexed { index, type -> type to index }
        .let { sequence ->
            MutableObjectIntMap<KClass<*>>().apply {
                sequence.forEach {
                    put(it.first, it.second)
                }
            }
        }

}