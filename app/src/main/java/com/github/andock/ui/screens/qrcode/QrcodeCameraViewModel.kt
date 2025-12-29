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
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalGetImage::class)
@HiltViewModel
class QrcodeCameraViewModel @Inject constructor(
    private val application: Application
) : ViewModel(), ImageAnalysis.Analyzer {
    private val barcodeScanner = BarcodeScanning.getClient()
    private val preview = Preview.Builder().build()
    private val imageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()

    init {
        imageAnalysis.setAnalyzer(
            Dispatchers.IO.asExecutor(),
            this
        )
    }

    @Volatile
    var onBarcodeDetected: ((Barcode) -> Unit)? = null

    init {
        viewModelScope.launch {
            var provider: ProcessCameraProvider? = null
            try {
                provider = ProcessCameraProvider.getInstance(application).await()
                awaitCancellation()
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
        val provider = ProcessCameraProvider
            .getInstance(application)
            .await()
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

    @Deprecated("ImageAnalysis use only", level = DeprecationLevel.HIDDEN)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        val onBarcodeDetected = onBarcodeDetected
        if (mediaImage != null && onBarcodeDetected != null) {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val image = InputImage.fromMediaImage(
                mediaImage,
                rotationDegrees
            )
            val callbacks = object : OnSuccessListener<List<Barcode>>,
                OnCompleteListener<List<Barcode>> {
                override fun onSuccess(barcodes: List<Barcode>) {
                    for (barcode in barcodes) {
                        onBarcodeDetected(barcode)
                    }
                }

                override fun onComplete(p0: Task<List<Barcode>>) {
                    imageProxy.close()
                }
            }
            barcodeScanner.process(image)
                .addOnSuccessListener(callbacks)
                .addOnCompleteListener(callbacks)
        } else {
            imageProxy.close()
        }
    }
}