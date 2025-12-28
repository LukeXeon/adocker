package com.github.andock.ui.screens.qrcode

import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.guava.asDeferred
import timber.log.Timber

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreview(
    flashEnabled: Boolean,
    onBarcodeDetected: (Barcode) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val (previewView, setPreviewView) = remember { mutableStateOf<PreviewView?>(null) }
    val (cameraProvider, setCameraProvider) = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    LaunchedEffect(Unit) {
        setCameraProvider(ProcessCameraProvider.getInstance(context).asDeferred().await())
    }
    if (previewView != null && cameraProvider != null) {
        DisposableEffect(previewView) {
            val barcodeScanner = BarcodeScanning.getClient()
            // Preview use case
            val preview = Preview.Builder()
                .build()
                .also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
            // Image analysis use case for barcode scanning
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply {
                    setAnalyzer(Dispatchers.IO.asExecutor()) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            barcodeScanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        onBarcodeDetected(barcode)
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }
                }

            // Camera selector
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind all use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                // Enable/disable flash
                camera.cameraControl.enableTorch(flashEnabled)
            } catch (e: Exception) {
                Timber.e(e)
            }
            onDispose {
                cameraProvider.unbindAll()
                barcodeScanner.close()
            }
        }
        // Update flash when changed
        LaunchedEffect(flashEnabled) {
            try {
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA
                )
                camera.cameraControl.enableTorch(flashEnabled)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
    AndroidView(
        factory = ::PreviewView,
        modifier = Modifier.Companion.fillMaxSize()
    ) { view ->
        setPreviewView(view)
    }
}