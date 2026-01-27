package com.github.andock.ui.screens.images

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.github.andock.daemon.images.ImageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ImageDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val imageManager: ImageManager,
) : ViewModel() {
    val imageId = savedStateHandle.toRoute<ImageDetailKey>().imageId

    val image = imageManager.getImageById(imageId).stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        null
    )

    suspend fun deleteImage(id: String) {
        imageManager.deleteImage(id)
    }
}