package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.scanner.QrBarcodeAnalyzer
import com.example.utils.GalleryPickerHelper
import com.example.viewmodel.MainViewModel
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Automatically re-check camera permission on ON_RESUME (e.g. after returning from Settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Camera permission not granted. You can enable it in App Settings.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            try {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    // Standard ActivityResult launcher using GalleryPickerHelper (ACTION_PICK + ACTION_GET_CONTENT)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (uri != null) {
            viewModel.scanImageUri(uri)
        }
    }

    val onPickGallerySafe: () -> Unit = {
        GalleryPickerHelper.launchImagePicker(context, galleryLauncher)
    }

    val onOpenSettings: () -> Unit = {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Please open Android Settings > Apps > OfflineQR > Permissions > Camera", Toast.LENGTH_LONG).show()
        }
    }

    if (!hasCameraPermission) {
        CameraPermissionDeniedContent(
            onRequestPermission = {
                try {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                } catch (t: Throwable) {
                    onOpenSettings()
                }
            },
            onOpenSettings = onOpenSettings,
            onPickGallery = onPickGallerySafe,
            modifier = modifier
        )
    } else {
        CameraScannerContent(
            viewModel = viewModel,
            onPickGallery = onPickGallerySafe,
            modifier = modifier
        )
    }
}

@Composable
fun CameraScannerContent(
    viewModel: MainViewModel,
    onPickGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val torchEnabled by viewModel.torchEnabled.collectAsStateWithLifecycle()
    val isFrontCamera by viewModel.isFrontCamera.collectAsStateWithLifecycle()

    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraExecutor.shutdown()
            } catch (ignored: Throwable) {}
        }
    }

    // Bind camera in LaunchedEffect, NOT on every Compose recomposition
    LaunchedEffect(isFrontCamera, previewViewRef, lifecycleOwner) {
        val pView = previewViewRef ?: return@LaunchedEffect
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = pView.surfaceProvider
                }

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(
                            cameraExecutor,
                            QrBarcodeAnalyzer { text, format ->
                                viewModel.onCodeScanned(text, format)
                            }
                        )
                    }

                val hasFront = cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
                val hasBack = cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)

                val selector = if (isFrontCamera && hasFront) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else if (hasBack) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else if (hasFront) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    cameraError = "No camera found on this device or emulator."
                    return@addListener
                }

                cameraInstance = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    imageAnalyzer
                )
                cameraError = null
            } catch (t: Throwable) {
                t.printStackTrace()
                cameraError = "Camera could not be accessed. Please check permissions."
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Toggle torch safely
    LaunchedEffect(torchEnabled, cameraInstance) {
        cameraInstance?.let { cam ->
            try {
                if (cam.cameraInfo.hasFlashUnit()) {
                    cam.cameraControl.enableTorch(torchEnabled)
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("camera_preview")
    ) {
        if (cameraError != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.VideocamOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Camera Unavailable",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = cameraError ?: "Camera could not be started.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onPickGallery,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan from Photo Gallery")
                }
            }
        } else {
            // Camera Preview View
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }.also { previewViewRef = it }
                },
                update = { /* Do not rebind camera on recomposition */ },
                modifier = Modifier.fillMaxSize()
            )

            // Safe overlay with framing and animated laser line
            ScannerOverlay()
        }

        // Top Controls (Status badge, Torch & Camera Switch)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF00F5D4), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "OFFLINE SCAN",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalIconButton(
                    onClick = { viewModel.toggleTorch() },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (torchEnabled) Color(0xFF00B4D8) else Color.Black.copy(alpha = 0.6f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("torch_button")
                ) {
                    Icon(
                        imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flashlight"
                    )
                }

                FilledTonalIconButton(
                    onClick = { viewModel.toggleCameraLens() },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.6f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("camera_switch_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera"
                    )
                }
            }
        }

        // Bottom Controls (Scan from gallery button & tip)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "Align QR code or Barcode inside the frame",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onPickGallery,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .testTag("gallery_picker_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Scan from Photo Gallery",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun ScannerOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "laser_transition")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_position"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val boxSize = (minOf(maxWidth, maxHeight) * 0.72f).coerceAtMost(320.dp)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val frameSizePx = boxSize.toPx()
            val left = (canvasWidth - frameSizePx) / 2f
            val top = (canvasHeight - frameSizePx) / 2.3f
            val right = left + frameSizePx
            val bottom = top + frameSizePx

            // Crash-safe 4-quadrant scrim around the frame
            val scrimColor = Color.Black.copy(alpha = 0.55f)
            drawRect(scrimColor, Offset(0f, 0f), Size(canvasWidth, top))
            drawRect(scrimColor, Offset(0f, bottom), Size(canvasWidth, canvasHeight - bottom))
            drawRect(scrimColor, Offset(0f, top), Size(left, frameSizePx))
            drawRect(scrimColor, Offset(right, top), Size(canvasWidth - right, frameSizePx))

            // Corner brackets
            val bracketLength = 36.dp.toPx()
            val strokeWidth = 4.dp.toPx()
            val cornerColor = Color(0xFF38BDF8)

            // Top Left
            drawLine(cornerColor, Offset(left, top), Offset(left + bracketLength, top), strokeWidth)
            drawLine(cornerColor, Offset(left, top), Offset(left, top + bracketLength), strokeWidth)

            // Top Right
            drawLine(cornerColor, Offset(right, top), Offset(right - bracketLength, top), strokeWidth)
            drawLine(cornerColor, Offset(right, top), Offset(right, top + bracketLength), strokeWidth)

            // Bottom Left
            drawLine(cornerColor, Offset(left, bottom), Offset(left + bracketLength, bottom), strokeWidth)
            drawLine(cornerColor, Offset(left, bottom), Offset(left, bottom - bracketLength), strokeWidth)

            // Bottom Right
            drawLine(cornerColor, Offset(right, bottom), Offset(right - bracketLength, bottom), strokeWidth)
            drawLine(cornerColor, Offset(right, bottom), Offset(right, bottom - bracketLength), strokeWidth)

            // Animated Laser line
            val laserY = top + (frameSizePx * laserPosition)
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF00F5D4),
                        Color.White,
                        Color(0xFF00F5D4),
                        Color.Transparent
                    ),
                    startX = left,
                    endX = right
                ),
                topLeft = Offset(left, laserY - 1.5.dp.toPx()),
                size = Size(frameSizePx, 3.dp.toPx())
            )
        }
    }
}

@Composable
fun CameraPermissionDeniedContent(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onPickGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.VideocamOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Camera Permission Required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "To scan codes using the camera, OfflineQR needs camera access. If Android previously blocked the prompt, tap 'Open App Settings' to enable Camera.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Primary action: Request Permission
            Button(
                onClick = onRequestPermission,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(48.dp)
                    .testTag("grant_camera_permission_button")
            ) {
                Icon(imageVector = Icons.Default.Videocam, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Allow Camera Permission")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Open App Settings directly
            OutlinedButton(
                onClick = onOpenSettings,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open App Settings")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pick from Gallery
            FilledTonalButton(
                onClick = onPickGallery,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.Image, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan from Photo Gallery")
            }
        }
    }
}
