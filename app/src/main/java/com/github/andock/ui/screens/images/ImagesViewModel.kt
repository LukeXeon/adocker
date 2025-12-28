package com.github.andock.ui.screens.images

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.andock.daemon.images.ImageDownloader
import com.github.andock.daemon.images.ImageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImagesViewModel @Inject constructor(
    private val imageManager: ImageManager,
) : ViewModel() {

    val images
        get() = imageManager.images

    fun getImageById(id: String) = imageManager.getImageById(id)

    fun pullImage(imageName: String): ImageDownloader {
        return imageManager.pullImage(imageName)
    }

    fun deleteImage(id: String) {
        viewModelScope.launch {
            imageManager.deleteImage(id)
        }
    }
}