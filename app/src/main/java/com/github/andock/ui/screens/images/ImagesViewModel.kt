package com.github.andock.ui.screens.images

import androidx.lifecycle.ViewModel
import com.github.andock.daemon.images.ImageManager
import com.github.andock.daemon.images.ImageReference
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ImagesViewModel @Inject constructor(
    private val imageManager: ImageManager,
) : ViewModel() {

    val images
        get() = imageManager.images

    fun pullImage(imageRef: ImageReference) = imageManager.pullImage(imageRef)

    suspend fun deleteImage(id: String) {
        imageManager.deleteImage(id)
    }
}