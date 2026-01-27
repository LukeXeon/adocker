package com.github.andock.ui.screens.images

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.andock.daemon.images.ImageManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel(assistedFactory = ImageDetailViewModel.Factory::class)
class ImageDetailViewModel @AssistedInject constructor(
    @Assisted private val navKey: ImageDetailKey,
    private val imageManager: ImageManager,
) : ViewModel() {
    val imageId = navKey.imageId

    val image = imageManager.getImageById(imageId).stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        null
    )

    suspend fun deleteImage(id: String) {
        imageManager.deleteImage(id)
    }

    @AssistedFactory
    interface Factory {
        fun create(navKey: ImageDetailKey): ImageDetailViewModel
    }
}