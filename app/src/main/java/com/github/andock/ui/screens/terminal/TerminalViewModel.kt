package com.github.andock.ui.screens.terminal

import androidx.lifecycle.ViewModel
import com.github.andock.daemon.containers.ContainerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val containerManager: ContainerManager
) : ViewModel() {

}