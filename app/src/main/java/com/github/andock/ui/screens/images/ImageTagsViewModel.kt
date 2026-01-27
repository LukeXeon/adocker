package com.github.andock.ui.screens.images

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.github.andock.daemon.images.ImageManager
import com.github.andock.daemon.images.ImageReference
import com.github.andock.daemon.images.ImageRepositories
import com.github.andock.daemon.registries.RegistryModule
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel(assistedFactory = ImageTagsViewModel.Factory::class)
class ImageTagsViewModel @AssistedInject constructor(
    private val imageManager: ImageManager,
    @Assisted private val navKey: ImageTagsKey,
    repositories: ImageRepositories,
) : ViewModel() {
    val repository = navKey.repository

    val tags = repositories[RegistryModule.DEFAULT_REGISTRY]
        .tags(repository)
        .cachedIn(viewModelScope)

    fun pullImage(imageRef: ImageReference) = imageManager.pullImage(imageRef)

    @AssistedFactory
    interface Factory {
        fun create(navKey: ImageTagsKey): ImageTagsViewModel
    }
}