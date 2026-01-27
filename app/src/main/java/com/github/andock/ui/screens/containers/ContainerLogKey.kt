package com.github.andock.ui.screens.containers

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class ContainerLogKey(
    val containerId: String
) : NavKey