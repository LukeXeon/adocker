package com.github.andock.ui.screens.main

import androidx.lifecycle.ViewModel
import com.github.andock.ui.screens.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

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
    }.sortedBy { it.second.priority }.toList()
}