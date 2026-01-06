package com.github.andock.ui.screens.main

import androidx.collection.ObjectIntMap
import androidx.lifecycle.ViewModel
import com.github.andock.ui.route.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.reflect.KClass

@HiltViewModel
class MainViewModel @Inject constructor(
    screens: Map<Class<*>, Screen>,
    bottomTabs: Map<Class<*>, MainBottomTab>,
    order: ObjectIntMap<KClass<*>>,
) : ViewModel() {
    val screens = screens.asSequence().map {
        it.key.kotlin to it.value
    }.toList()
    val bottomTabs = bottomTabs.asSequence().map {
        it.key.kotlin to it.value
    }.sortedBy { order.getOrDefault(it.first, -1) }.toList()
}