package com.github.andock.ui.screens.qrcode

import android.app.Application
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalGetImage::class)
@HiltViewModel
class QrcodeScannerModel @Inject constructor(
    private val application: Application
) : ViewModel(), ImageAnalysis.Analyzer {
    private val barcodeScanner = BarcodeScanning.getClient()
    private val preview = Preview.Builder().build()
    private val imageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .apply {
            setAnalyzer(Dispatchers.IO.asExecutor(), this@QrcodeScannerModel)
        }
    var onBarcodeDetected: ((Barcode) -> Unit)? = null

    init {
        viewModelScope.launch {
            var provider: ProcessCameraProvider? = null
            try {
                provider = ProcessCameraProvider.getInstance(application).await()
            } finally {
                provider?.unbindAll()
                barcodeScanner.close()
            }
        }
    }

    fun setSurface(surfaceProvider: Preview.SurfaceProvider?) {
        preview.surfaceProvider = surfaceProvider
    }

    suspend fun setupCamera(
        lifecycleOwner: LifecycleOwner,
    ): Camera {
        val provider = ProcessCameraProvider.getInstance(application).await()
        // Unbind all use cases before rebinding
        provider.unbindAll()
        // Bind use cases to camera
        return provider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageAnalysis
        )
    }

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        val onBarcodeDetected = onBarcodeDetected
        if (mediaImage != null && onBarcodeDetected != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )
            barcodeScanner.process(image).addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    onBarcodeDetected(barcode)
                }
            }.addOnCompleteListener {
                imageProxy.close()
            }
        } else {
            imageProxy.close()
        }
    }
}