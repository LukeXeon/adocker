package com.github.andock.ui.screens.qrcode

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.core.content.ContextCompat
import com.github.andock.R
import com.github.andock.ui.screens.main.LocalNavigator
import com.github.andock.ui.screens.main.LocalResultEventBus
import com.github.andock.ui.screens.main.ResultEventBus
import com.github.andock.ui.utils.debounceClick
import kotlinx.coroutines.launch

val scannedData by ResultEventBus.key<String?>(null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrcodeScannerScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val bus = LocalResultEventBus.current
    val scope = rememberCoroutineScope()
    val (hasCameraPermission, setHasCameraPermission) = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val onNavigateBack = debounceClick {
        navigator.goBack()
    }
    val (flashEnabled, setFlashEnabled) = remember { mutableStateOf(false) }
    val (scannedData, setScannedData) = remember { mutableStateOf<String?>(null) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        setHasCameraPermission(isGranted)
    }
    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(
            WindowInsetsSides.Top
        ),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.qr_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    if (hasCameraPermission) {
                        IconButton(onClick = { setFlashEnabled(!flashEnabled) }) {
                            Icon(
                                if (flashEnabled) {
                                    Icons.Default.FlashOn
                                } else {
                                    Icons.Default.FlashOff
                                },
                                contentDescription = null
                            )
                        }
                    }
                }
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (hasCameraPermission) {
                // Camera preview
                QrcodeCamera(
                    flashEnabled = flashEnabled,
                    onBarcodeDetected = { barcode ->
                        val data = barcode.rawValue
                        if (data != null && scannedData != data) {
                            setScannedData(data)
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
    if (scannedData != null) {
        QrcodeConfirmDialog(
            scannedData = scannedData,
            onConfirm = {
                scope.launch {
                    setScannedData(null)
                    bus.send(com.github.andock.ui.screens.qrcode.scannedData, scannedData)
                    onNavigateBack()
                }
            },
            onDismissRequest = {
                setScannedData(null)
            }
        )
    }
}
