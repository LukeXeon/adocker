package com.github.andock.daemon.images

import com.github.andock.daemon.client.ImageReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageManager @Inject constructor(
    scope: CoroutineScope,
    private val downloaderFactory: ImageDownloader.Factory
) {
    private val _images = MutableStateFlow<Map<String, Image>>(emptyMap())

    val images = _images.asStateFlow()
    val sortedList = _images.map {
        it.asSequence().sortedBy { image -> image.key }.map { image -> image.value }.toList()
    }.stateIn(
        scope,
        SharingStarted.Eagerly,
        emptyList()
    )

    fun pull(imageName: String): ImageDownloader {
        return downloaderFactory.create(ImageReference.parse(imageName))
    }
}