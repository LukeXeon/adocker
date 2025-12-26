package com.github.andock.daemon.images

import com.github.andock.daemon.database.model.ImageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ImageDownloader constructor(

    private val imageReference: ImageEntity,
    scope: CoroutineScope
) {
    private val _state = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())

    val state = _state.map {
        it.asSequence()
            .sortedBy { item -> item.key }
            .map { item -> item.value }
            .toList()
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        scope.launch {

        }
    }

//    @Singleton
//    @AssistedFactory
//    interface Factory {
//        fun create(
//            @Assisted
//            imageReference: ImageReference,
//        ): ImageDownloader
//    }
}