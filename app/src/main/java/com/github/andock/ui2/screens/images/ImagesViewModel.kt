package com.github.andock.ui2.screens.images

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.andock.daemon.images.ImageRepository
import com.github.andock.daemon.images.PullProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImagesViewModel @Inject constructor(
    private val imageRepository: ImageRepository,
) : ViewModel() {

    val images = imageRepository.getAllImages()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Pull progress
    private val _pullProgress = MutableStateFlow<Map<String, PullProgress>>(emptyMap())
    val pullProgress: StateFlow<Map<String, PullProgress>> = _pullProgress.asStateFlow()

    private val _isPulling = MutableStateFlow(false)
    val isPulling: StateFlow<Boolean> = _isPulling.asStateFlow()

    fun deleteImage(imageId: String) {
        viewModelScope.launch {
            imageRepository.deleteImage(imageId)
                .onSuccess {
//                    _message.value = "Image deleted successfully"
                }
                .onFailure { e ->
//                    _error.value = "Delete failed: ${e.message}"
                }
        }
    }
}