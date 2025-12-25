package com.github.andock.ui.screens.images

import androidx.lifecycle.ViewModel
import com.github.andock.daemon.images.ImageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ImagesViewModel @Inject constructor(
    private val imageManager: ImageManager,
) : ViewModel() {

    val images = imageManager.images
    val sortedList = imageManager.sortedList

    fun deleteImage(imageId: String) {

    }
}