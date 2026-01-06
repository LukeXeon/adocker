package com.github.andock.ui.screens.main

import androidx.lifecycle.ViewModel
import com.github.andock.ui.route.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    screens: Map<Class<*>, Screen>,
    val bottomTabs: List<@JvmSuppressWildcards MainBottomTab<*>>,
) : ViewModel() {
    val screens = screens.asSequence().map {
        it.key.kotlin to it.value
    }.toList()
}