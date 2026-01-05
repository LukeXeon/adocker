package com.github.andock.ui.screens.images

import androidx.collection.LruCache
import androidx.lifecycle.ViewModel
import com.github.andock.daemon.images.ImageManager
import com.github.andock.daemon.images.ImageReference
import com.github.andock.daemon.images.ImageRepository
import com.github.andock.daemon.registries.RegistryModule
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ImagesViewModel @Inject constructor(
    private val imageManager: ImageManager,
    private val repositories: LruCache<String, ImageRepository>,
) : ViewModel() {

    val images
        get() = imageManager.images
    private val imageRepository
        get() = repositories[RegistryModule.DEFAULT_REGISTRY]!!

    fun getImageById(id: String) = imageManager.getImageById(id)

    fun pullImage(imageRef: ImageReference) = imageManager.pullImage(imageRef)

    fun tags(repository: String) = imageRepository.tags(repository)

    suspend fun deleteImage(id: String) {
        imageManager.deleteImage(id)
    }
}