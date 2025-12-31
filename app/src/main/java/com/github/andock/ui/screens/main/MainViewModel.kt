package com.github.andock.ui.screens.main

import androidx.lifecycle.ViewModel
import com.github.andock.ui.screens.Screen
import com.github.andock.ui.screens.containers.ContainersRoute
import com.github.andock.ui.screens.home.HomeRoute
import com.github.andock.ui.screens.images.ImagesRoute
import com.github.andock.ui.screens.search.SearchRoute
import com.github.andock.ui.screens.settings.SettingsRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.reflect.KClass

@HiltViewModel
class MainViewModel @Inject constructor(
    screens: Map<Class<*>, Screen>,
    bottomTabs: Map<Class<*>, MainBottomTab>,
) : ViewModel() {
    val screens = screens.asSequence().map {
        it.key.kotlin to it.value
    }.toList()
    val bottomTabs = bottomTabs.asSequence().map {
        it.key.kotlin to it.value
    }.sortedBy { order[it.first] ?: -1 }.toList()

    companion object {
        private val order = arrayOf<KClass<*>>(
            HomeRoute::class,
            SearchRoute::class,
            ContainersRoute::class,
            ImagesRoute::class,
            SettingsRoute::class
        ).asSequence().mapIndexed { index, type -> type to index }
            .toMap()
    }
}