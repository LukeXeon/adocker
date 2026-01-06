package com.github.andock.ui.screens.images

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import androidx.paging.cachedIn
import com.github.andock.daemon.images.ImageManager
import com.github.andock.daemon.images.ImageReference
import com.github.andock.daemon.images.ImageRepositories
import com.github.andock.daemon.registries.RegistryModule
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ImageTagsViewModel @Inject constructor(
    private val imageManager: ImageManager,
    savedStateHandle: SavedStateHandle,
    repositories: ImageRepositories,
) : ViewModel() {
    val repository = savedStateHandle.toRoute<ImageTagsRoute>().repository

    val tags = repositories[RegistryModule.DEFAULT_REGISTRY]
        .tags(repository)
        .cachedIn(viewModelScope)

    fun pullImage(imageRef: ImageReference) = imageManager.pullImage(imageRef)

}