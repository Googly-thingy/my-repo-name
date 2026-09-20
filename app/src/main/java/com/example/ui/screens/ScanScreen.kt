package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.model3d.Mesh3D
import com.example.photogrammetry.CapturedPhoto
import com.example.photogrammetry.ReconstructionManager
import com.example.ui.theme.StudioAccent
import com.example.ui.theme.StudioAccentMuted
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAmberBg
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorderSubtle
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioEmeraldBg
import com.example.ui.theme.StudioPrimary
import com.example.ui.theme.StudioSurfaceMuted
import com.example.ui.theme.StudioSurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ScanScreen(
    reconstructionManager: ReconstructionManager,
    onMeshGenerated: (Mesh3D) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val capturedPhotos by reconstructionManager.capturedPhotos.collectAsState()
    val isReconstructing by reconstructionManager.isReconstructing.collectAsState()
    val progress by reconstructionManager.progress.collectAsState()
    val qualityMetrics by reconstructionManager.qualityMetrics.collectAsState()

    var currentAngleDeg by remember { mutableFloatStateOf(0f) }
    var currentElevationDeg by remember { mutableFloatStateOf(30f) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // Photo gallery batch picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            reconstructionManager.addBatchPhotos(uris.size.coerceIn(12, 36))
        }
    }

    var showTipsDialog by remember { mutableStateOf(false) }

    // Cleanly unbind camera when navigating away or disposing composable to avoid BufferQueue abandoned errors
    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                // Ignore if camera provider not initialized
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Photogrammetry 3D Scanner",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = "Turn real objects into high-quality 3D meshes",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            IconButton(
                onClick = { showTipsDialog = !showTipsDialog },
                modifier = Modifier
                    .size(38.dp)
                    .background(if (showTipsDialog) StudioAmberBg else StudioSurfaceWhite, CircleShape)
                    .shadow(1.dp, CircleShape, spotColor = Color(0x0F0F172A))
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Scan Tips",
                    tint = StudioAmber,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Beginner Tips Card (Expandable)
        AnimatedVisibility(visible = showTipsDialog) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = StudioAmberBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(14.dp), spotColor = Color(0x0A0F172A))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Beginner Photogrammetry Tips:", color = StudioAmber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("• Circle smoothly around the object at even distances (~40-60 cm)", color = TextPrimary, fontSize = 12.sp)
                    Text("• Ensure at least 60-80% overlap between adjacent exposures", color = TextPrimary, fontSize = 12.sp)
                    Text("• Avoid transparent, reflective glass, or featureless white surfaces", color = TextPrimary, fontSize = 12.sp)
                    Text("• Use soft, diffuse ambient light without harsh dynamic shadows", color = TextPrimary, fontSize = 12.sp)
                }
            }
        }

        // Viewfinder Container with Circular Orbit Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
                .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = Color(0x150F172A))
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F172A))
        ) {
            // Live Rear Camera Viewfinder or Permission Prompt
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            // Use COMPATIBLE mode (TextureView) to prevent SurfaceView BufferQueue abandoned errors in Compose
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }.also { previewView ->
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                try {
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    cameraProvider.unbindAll()
                                    // Strictly enforce DEFAULT_BACK_CAMERA for photogrammetry capture
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview
                                    )
                                } catch (e: Exception) {
                                    // Camera in use or running in emulator
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                        }
                    },
                    onRelease = { previewView ->
                        try {
                            val cameraProvider = ProcessCameraProvider.getInstance(previewView.context).get()
                            cameraProvider.unbindAll()
                        } catch (e: Exception) {
                            // Ignore
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Permission Request Callout Overlay
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = StudioAccent,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Rear Camera Access Required",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Photogrammetry requires the rear camera sensor to capture accurate geometric depth & feature parallax.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(vertical = 6.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioAccent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("request_camera_permission_button")
                    ) {
                        Text("Enable Rear Camera", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Interactive Photogrammetry Guidance HUD Canvas (Orbit compass & Feature Points)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val center = Offset(w * 0.5f, h * 0.5f)
                val radius = w.coerceAtMost(h) * 0.38f

                // Draw central targeting reticle
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f),
                    radius = 28f,
                    center = center,
                    style = Stroke(width = 1.5f)
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(center.x - 40f, center.y),
                    end = Offset(center.x + 40f, center.y),
                    strokeWidth = 1.2f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(center.x, center.y - 40f),
                    end = Offset(center.x, center.y + 40f),
                    strokeWidth = 1.2f
                )

                // Draw 360-degree circular orbit ring
                drawCircle(
                    color = Color.White.copy(alpha = 0.2f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2.5f)
                )

                // 24 Angular sector ticks around the orbit ring
                val tickCount = 24
                for (i in 0 until tickCount) {
                    val angle = (i.toFloat() / tickCount) * 2f * Math.PI.toFloat()
                    val tickAngleDeg = (i * 360f / tickCount)
                    val pOuter = Offset(center.x + radius * cos(angle), center.y + radius * sin(angle))
                    val pInner = Offset(center.x + (radius - 12f) * cos(angle), center.y + (radius - 12f) * sin(angle))

                    // Check if this sector has been captured
                    val isCaptured = capturedPhotos.any {
                        val diff = kotlin.math.abs(it.angleDeg - tickAngleDeg)
                        diff < 15f || diff > 345f
                    }

                    val tickColor = if (isCaptured) StudioEmerald else Color.White.copy(alpha = 0.35f)
                    drawLine(tickColor, pInner, pOuter, strokeWidth = if (isCaptured) 3f else 1.2f)

                    if (isCaptured) {
                        drawCircle(StudioEmerald, radius = 3.5f, center = pOuter)
                    }
                }

                // Current Camera Azimuth indicator
                val currentRad = Math.toRadians(currentAngleDeg.toDouble()).toFloat()
                val camPos = Offset(center.x + radius * cos(currentRad), center.y + radius * sin(currentRad))
                drawCircle(StudioAccent, radius = 7f, center = camPos)
                drawCircle(Color.White, radius = 3.5f, center = camPos)

                // Simulated holographic SIFT/ORB feature points on the subject
                val featureDots = listOf(
                    Offset(center.x - 30f, center.y - 20f),
                    Offset(center.x + 25f, center.y - 35f),
                    Offset(center.x + 10f, center.y + 25f),
                    Offset(center.x - 20f, center.y + 15f),
                    Offset(center.x + 35f, center.y + 10f),
                    Offset(center.x - 10f, center.y - 45f)
                )
                for (pt in featureDots) {
                    drawCircle(StudioAccent.copy(alpha = 0.8f), radius = 2.5f, center = pt)
                }
            }

            // Top Status Overlay (Photo count, Rear Camera Badge, & Angle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = "Exposures: ${capturedPhotos.size} / 24",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (hasCameraPermission) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.65f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(StudioEmerald, CircleShape)
                                )
                                Text(
                                    text = "REAR CAM",
                                    color = StudioEmerald,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = "Azimuth: ${currentAngleDeg.toInt()}°",
                        color = Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Bottom Guidance Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.75f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = StudioEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = qualityMetrics.guidanceMessage,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Quality Feedback Telemetry Bar (Overlap, Lighting, Stability)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = StudioSurfaceWhite,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(14.dp), spotColor = Color(0x0A0F172A))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Overlap
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Overlap", color = TextTertiary, fontSize = 11.sp)
                    Text("${qualityMetrics.overlapPercent}%", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Optimal", color = StudioEmerald, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
                // Lighting
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Lighting", color = TextTertiary, fontSize = 11.sp)
                    Text("Diffuse", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Low Glare", color = StudioEmerald, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
                // Motion Stability
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Stability", color = TextTertiary, fontSize = 11.sp)
                    Text("Steady", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Sharp Focus", color = StudioEmerald, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Capture Controls & Shutter Buttons
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Primary Shutter: Capture Single Angle
                Button(
                    onClick = {
                        reconstructionManager.addCapturedPhoto(currentAngleDeg, currentElevationDeg)
                        // Advance angle clockwise by 15°
                        currentAngleDeg = (currentAngleDeg + 15f) % 360f
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("capture_shutter_button")
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Capture Angle", fontWeight = FontWeight.Bold)
                }

                // Turntable Auto 360 Batch Capture
                FilledTonalButton(
                    onClick = {
                        reconstructionManager.addBatchPhotos(16)
                        currentAngleDeg = 0f
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = StudioAccentMuted,
                        contentColor = StudioPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("auto_orbit_button")
                ) {
                    Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Auto 360° Scan", fontWeight = FontWeight.Bold)
                }
            }

            // Secondary Actions: Import Multi-Photos & Clear
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorderSubtle),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                ) {
                    Icon(Icons.Default.Collections, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Import Photos", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                if (capturedPhotos.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { reconstructionManager.clearPhotos() },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorderSubtle),
                        modifier = Modifier
                            .weight(0.6f)
                            .height(42.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reset", color = TextSecondary, fontSize = 12.sp)
                    }
                }

                if (!hasCameraPermission) {
                    OutlinedButton(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Text("Enable Camera", color = TextPrimary, fontSize = 12.sp)
                    }
                }
            }
        }

        // Photogrammetry Reconstruction Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = StudioSurfaceWhite,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = Color(0x0A0F172A))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("3D Mesh Reconstruction", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            text = if (capturedPhotos.size >= 8) "Coverage sufficient to solve camera geometry" else "Need at least 8 photo angles to reconstruct",
                            color = if (capturedPhotos.size >= 8) StudioEmerald else TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (capturedPhotos.size >= 8) StudioAccent else TextTertiary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Progress Bar if Reconstructing
                if (isReconstructing) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(progress.stage.title, color = StudioAccent, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text("${(progress.progressPercent * 100).toInt()}%", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        LinearProgressIndicator(
                            progress = { progress.progressPercent },
                            color = StudioAccent,
                            trackColor = Color(0xFFE2E8F0),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        Text(
                            text = progress.logMessage,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                // Reconstruction Action Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            reconstructionManager.runReconstruction("Photogrammetry Scan #${capturedPhotos.size}") { generatedMesh ->
                                onMeshGenerated(generatedMesh)
                            }
                        }
                    },
                    enabled = !isReconstructing && capturedPhotos.size >= 4,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("reconstruct_mesh_button")
                ) {
                    if (isReconstructing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Solving Multi-View Stereo...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (capturedPhotos.size >= 4) "Generate 3D Mesh (${capturedPhotos.size} Photos)" else "Capture 4+ Photos to Reconstruct",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
