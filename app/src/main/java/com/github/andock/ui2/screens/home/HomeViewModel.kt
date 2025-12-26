package com.github.andock.ui2.screens.home

import androidx.lifecycle.ViewModel
import com.github.andock.daemon.containers.ContainerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    containerManager: ContainerManager,
) : ViewModel() {

    val stats = MutableStateFlow(  HomeStats(
    ))
}