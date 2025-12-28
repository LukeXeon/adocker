package com.github.andock.ui.screens.qrcode

import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.guava.await


@OptIn(ExperimentalGetImage::class)
@Composable
fun QrcodeCamera(
    flashEnabled: Boolean,
    onBarcodeDetected: (Barcode) -> Unit
) {
    val viewModel = hiltViewModel<QrcodeCameraViewModel>()
    val lifecycleOwner = LocalLifecycleOwner.current
    val (previewView, setPreviewView) = remember { mutableStateOf<PreviewView?>(null) }
    val (camera, setCamera) = remember { mutableStateOf<Camera?>(null) }
    LaunchedEffect(Unit) {
        setCamera(viewModel.setupCamera(lifecycleOwner))
    }
    LaunchedEffect(onBarcodeDetected) {
        viewModel.onBarcodeDetected = onBarcodeDetected
    }
    LaunchedEffect(previewView) {
        viewModel.setSurface(previewView?.surfaceProvider)
    }
    if (camera != null) {
        LaunchedEffect(camera, flashEnabled) {
            camera.cameraControl.enableTorch(flashEnabled).await()
        }
    }
    AndroidView(
        factory = ::PreviewView,
        modifier = Modifier.fillMaxSize()
    ) { view ->
        setPreviewView(view)
    }
}