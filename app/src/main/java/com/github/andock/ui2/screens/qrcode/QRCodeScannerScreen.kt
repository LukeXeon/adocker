package com.github.andock.ui2.screens.qrcode

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.andock.R
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRCodeScannerScreen(
    onBarcodeScanned: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var flashEnabled by remember { mutableStateOf(false) }
    var scannedData by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.qr_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    if (hasCameraPermission) {
                        IconButton(onClick = { flashEnabled = !flashEnabled }) {
                            Icon(
                                if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = null
                            )
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (hasCameraPermission) {
                // Camera preview
                CameraPreview(
                    flashEnabled = flashEnabled,
                    onBarcodeDetected = { barcode ->
                        val data = barcode.rawValue
                        if (data != null && scannedData != data) {
                            scannedData = data
                            showConfirmDialog = true
                        }
                    }
                )

                // Overlay with scan frame
                val primaryColor = MaterialTheme.colorScheme.primary
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    val scanFrameSize = minOf(canvasWidth, canvasHeight) * 0.7f
                    val left = (canvasWidth - scanFrameSize) / 2
                    val top = (canvasHeight - scanFrameSize) / 2

                    // Draw dark overlay outside scan frame
                    drawRect(
                        color = Color.Black.copy(alpha = 0.5f),
                        size = size
                    )

                    // Clear scan frame area
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = Offset(left, top),
                        size = Size(scanFrameSize, scanFrameSize),
                        cornerRadius = CornerRadius(16.dp.toPx()),
                        blendMode = BlendMode.Clear
                    )

                    // Draw scan frame border
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(left, top),
                        size = Size(scanFrameSize, scanFrameSize),
                        cornerRadius = CornerRadius(16.dp.toPx()),
                        style = Stroke(width = 4.dp.toPx())
                    )

                    // Draw corner indicators
                    val cornerSize = 40.dp.toPx()
                    val cornerThickness = 6.dp.toPx()

                    // Top-left corner
                    drawLine(
                        color = primaryColor,
                        start = Offset(left, top + 16.dp.toPx()),
                        end = Offset(left, top),
                        strokeWidth = cornerThickness
                    )
                    drawLine(
                        color = primaryColor,
                        start = Offset(left, top),
                        end = Offset(left + cornerSize, top),
                        strokeWidth = cornerThickness
                    )

                    // Top-right corner
                    drawLine(
                        color = primaryColor,
                        start = Offset(left + scanFrameSize - cornerSize, top),
                        end = Offset(left + scanFrameSize, top),
                        strokeWidth = cornerThickness
                    )
                    drawLine(
                        color = primaryColor,
                        start = Offset(left + scanFrameSize, top),
                        end = Offset(left + scanFrameSize, top + cornerSize),
                        strokeWidth = cornerThickness
                    )

                    // Bottom-left corner
                    drawLine(
                        color = primaryColor,
                        start = Offset(left, top + scanFrameSize - cornerSize),
                        end = Offset(left, top + scanFrameSize),
                        strokeWidth = cornerThickness
                    )
                    drawLine(
                        color = primaryColor,
                        start = Offset(left, top + scanFrameSize),
                        end = Offset(left + cornerSize, top + scanFrameSize),
                        strokeWidth = cornerThickness
                    )

                    // Bottom-right corner
                    drawLine(
                        color = primaryColor,
                        start = Offset(left + scanFrameSize - cornerSize, top + scanFrameSize),
                        end = Offset(left + scanFrameSize, top + scanFrameSize),
                        strokeWidth = cornerThickness
                    )
                    drawLine(
                        color = primaryColor,
                        start = Offset(left + scanFrameSize, top + scanFrameSize - cornerSize),
                        end = Offset(left + scanFrameSize, top + scanFrameSize),
                        strokeWidth = cornerThickness
                    )
                }

                // Instruction text
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.qr_hint),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Permission denied UI
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.FlashOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.qr_permission_title),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.qr_permission_message),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                    ) {
                        Text(stringResource(R.string.qr_permission_grant))
                    }
                }
            }
        }
    }

    // Confirmation dialog
    if (showConfirmDialog && scannedData != null) {
        val parsedImage = parseImageFromQRCode(scannedData!!)

        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
                scannedData = null
            },
            icon = { Icon(Icons.Default.FlashOn, contentDescription = null) },
            title = { Text(stringResource(R.string.qr_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.qr_image_detected, parsedImage ?: scannedData!!))
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = parsedImage ?: scannedData!!,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onBarcodeScanned(parsedImage ?: scannedData!!)
                        showConfirmDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text(stringResource(R.string.images_pull))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        scannedData = null
                    }
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreview(
    flashEnabled: Boolean,
    onBarcodeDetected: (Barcode) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val barcodeScanner = remember { BarcodeScanning.getClient() }

    DisposableEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

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
                    setAnalyzer(Dispatchers.Default.asExecutor()) { imageProxy ->
                        processImageProxy(barcodeScanner, imageProxy, onBarcodeDetected)
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
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            cameraProviderFuture.get().unbindAll()
            barcodeScanner.close()
        }
    }

    // Update flash when changed
    LaunchedEffect(flashEnabled) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        try {
            val cameraProvider = cameraProviderFuture.get()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA
            )
            camera.cameraControl.enableTorch(flashEnabled)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    barcodeScanner: BarcodeScanner,
    imageProxy: ImageProxy,
    onBarcodeDetected: (Barcode) -> Unit
) {
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

/**
 * Parse image name from QR code data
 * Supports multiple formats:
 * - Simple: "ubuntu:22.04"
 * - JSON: {"type": "docker-image", "image": "ubuntu:22.04"}
 * - URL: "andock://pull?image=ubuntu:22.04"
 */
private fun parseImageFromQRCode(data: String): String? {
    return try {
        when {
            // JSON format
            data.trim().startsWith("{") -> {
                val json = Json.parseToJsonElement(data).jsonObject
                json["image"]?.jsonPrimitive?.content
            }
            // URL format
            data.startsWith("andock://pull?image=") -> {
                data.substringAfter("image=").substringBefore("&")
            }
            // Simple format (direct image name)
            else -> data
        }
    } catch (e: Exception) {
        // If parsing fails, return the raw data
        data
    }
}
