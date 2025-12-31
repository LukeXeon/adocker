package com.github.andock.ui.screens.images

import androidx.lifecycle.ViewModel
import com.github.andock.daemon.images.ImageManager
import com.github.andock.daemon.images.downloader.ImageDownloader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ImagesViewModel @Inject constructor(
    private val imageManager: ImageManager
) : ViewModel() {

    val images
        get() = imageManager.images

    fun getImageById(id: String) = imageManager.getImageById(id)

    fun pullImage(imageName: String): ImageDownloader {
        return imageManager.pullImage(imageName)
    }

    suspend fun deleteImage(id: String) {
        imageManager.deleteImage(id)
    }
}