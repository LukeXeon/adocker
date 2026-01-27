package com.github.andock.ui.screens.images

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class ImageTagsKey(
    val repository: String
) : NavKey