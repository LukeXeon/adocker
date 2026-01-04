package com.github.andock.ui.screens.images

import androidx.lifecycle.ViewModel
import com.github.andock.daemon.images.ImageClient
import com.github.andock.daemon.images.ImageManager
import com.github.andock.daemon.images.ImageReference
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ImagesViewModel @Inject constructor(
    private val imageManager: ImageManager,
    private val imageClient: ImageClient,
) : ViewModel() {

    val images
        get() = imageManager.images

    fun getImageById(id: String) = imageManager.getImageById(id)

    fun pullImage(imageName: ImageReference) = imageManager.pullImage(imageName)

    suspend fun getTags(
        repository: String
    ) = imageClient.getTags( repository)

    suspend fun deleteImage(id: String) {
        imageManager.deleteImage(id)
    }
}