package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animation.AnimationTrack
import com.example.animation.EasingType
import com.example.animation.FramerateSettings
import com.example.animation.Keyframe
import com.example.animation.MovementPreset
import com.example.model3d.Mesh3D
import com.example.model3d.ModelTransform
import com.example.ui.components.ExportDialog
import com.example.ui.components.FramerateSelector
import com.example.ui.components.TimelineBar
import com.example.ui.components.Viewport3D
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAccent
import com.example.ui.theme.StudioAccentMuted
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorderSubtle
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioPrimary
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceMuted
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.StudioSurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.ui.draw.shadow
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimateScreen(
    currentMesh: Mesh3D,
    modelLibrary: List<Mesh3D>,
    onSelectMesh: (Mesh3D) -> Unit,
    onOpenTutorial: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var track by remember { mutableStateOf(AnimationTrack()) }
    var currentTimeSec by remember { mutableFloatStateOf(0f) }
    var isPlaying by remember { mutableStateOf(true) }
    var selectedKeyframe by remember { mutableStateOf<Keyframe?>(track.keyframes.firstOrNull()) }
    var framerateSettings by remember { mutableStateOf(FramerateSettings(60)) }
    var activePreset by remember { mutableStateOf(MovementPreset.TURNTABLE_360) }
    var isOnionSkinEnabled by remember { mutableStateOf(true) }

    // Tucked Menu Visibility States
    var showPoseInspector by remember { mutableStateOf(false) }
    var showPresetSheet by remember { mutableStateOf(false) }
    var showFpsSheet by remember { mutableStateOf(false) }
    var showModelSheet by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    // Realtime playback loop strictly locked to FramerateSettings
    LaunchedEffect(isPlaying, framerateSettings.fps, track.durationSec, track.isLooping) {
        if (!isPlaying) return@LaunchedEffect
        val intervalMs = framerateSettings.frameDurationMs.toLong().coerceAtLeast(8L)
        val stepSec = framerateSettings.frameDurationSec

        while (isPlaying) {
            delay(intervalMs)
            val nextTime = currentTimeSec + stepSec
            if (nextTime >= track.durationSec) {
                if (track.isLooping) {
                    currentTimeSec = 0f
                } else {
                    currentTimeSec = track.durationSec
                    isPlaying = false
                }
            } else {
                currentTimeSec = nextTime
            }
        }
    }

    // Sample current model transform at current time
    val currentTransform: ModelTransform = remember(currentTimeSec, track) {
        track.sampleTransform(currentTimeSec)
    }

    // FlipaClip 3D Onion Skinning Ghosts: previous frame (Coral Red) & next frame (Electric Cyan)
    val onionSkins: List<Pair<ModelTransform, Color>> = remember(isOnionSkinEnabled, isPlaying, currentTimeSec, track) {
        if (!isOnionSkinEnabled || isPlaying || track.keyframes.size < 2) {
            emptyList()
        } else {
            val list = mutableListOf<Pair<ModelTransform, Color>>()
            val sorted = track.keyframes.sortedBy { it.timeSec }
            val currentIndex = sorted.indexOfFirst { kotlin.math.abs(it.timeSec - currentTimeSec) < 0.05f }

            if (currentIndex > 0) {
                val prevKf = sorted[currentIndex - 1]
                list.add(
                    Pair(
                        ModelTransform(
                            posX = prevKf.posX,
                            posY = prevKf.posY,
                            posZ = prevKf.posZ,
                            rotXDeg = prevKf.rotX,
                            rotYDeg = prevKf.rotY,
                            rotZDeg = prevKf.rotZ,
                            scaleX = prevKf.scale,
                            scaleY = prevKf.scale,
                            scaleZ = prevKf.scale,
                            squashStretch = prevKf.squashStretch,
                            twistDeg = prevKf.twistDeg,
                            bendX = prevKf.bendX
                        ),
                        Color(0x99FF5252) // Coral Red for previous frame
                    )
                )
            } else if (currentIndex == -1 && sorted.isNotEmpty()) {
                val prev = sorted.lastOrNull { it.timeSec < currentTimeSec }
                if (prev != null) {
                    list.add(
                        Pair(
                            ModelTransform(
                                posX = prev.posX,
                                posY = prev.posY,
                                posZ = prev.posZ,
                                rotXDeg = prev.rotX,
                                rotYDeg = prev.rotY,
                                rotZDeg = prev.rotZ,
                                scaleX = prev.scale,
                                scaleY = prev.scale,
                                scaleZ = prev.scale,
                                squashStretch = prev.squashStretch,
                                twistDeg = prev.twistDeg,
                                bendX = prev.bendX
                            ),
                            Color(0x99FF5252)
                        )
                    )
                }
            }

            if (currentIndex in 0 until sorted.size - 1) {
                val nextKf = sorted[currentIndex + 1]
                list.add(
                    Pair(
                        ModelTransform(
                            posX = nextKf.posX,
                            posY = nextKf.posY,
                            posZ = nextKf.posZ,
                            rotXDeg = nextKf.rotX,
                            rotYDeg = nextKf.rotY,
                            rotZDeg = nextKf.rotZ,
                            scaleX = nextKf.scale,
                            scaleY = nextKf.scale,
                            scaleZ = nextKf.scale,
                            squashStretch = nextKf.squashStretch,
                            twistDeg = nextKf.twistDeg,
                            bendX = nextKf.bendX
                        ),
                        Color(0x9900F0FF) // Electric Cyan for next frame
                    )
                )
            } else if (currentIndex == -1 && sorted.isNotEmpty()) {
                val next = sorted.firstOrNull { it.timeSec > currentTimeSec }
                if (next != null) {
                    list.add(
                        Pair(
                            ModelTransform(
                                posX = next.posX,
                                posY = next.posY,
                                posZ = next.posZ,
                                rotXDeg = next.rotX,
                                rotYDeg = next.rotY,
                                rotZDeg = next.rotZ,
                                scaleX = next.scale,
                                scaleY = next.scale,
                                scaleZ = next.scale,
                                squashStretch = next.squashStretch,
                                twistDeg = next.twistDeg,
                                bendX = next.bendX
                            ),
                            Color(0x9900F0FF)
                        )
                    )
                }
            }

            list
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Minimalist Premium Top Bar: Title + Tucked Option Trigger Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "3D Animation Studio",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    text = "${currentMesh.name} • ${framerateSettings.fps} FPS",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            // Tucked Menu Quick Pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tutorial Trigger
                Surface(
                    onClick = onOpenTutorial,
                    shape = RoundedCornerShape(12.dp),
                    color = StudioSurfaceWhite,
                    modifier = Modifier.shadow(2.dp, RoundedCornerShape(12.dp), spotColor = Color(0x0F0F172A))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Tutorial", tint = StudioAccent, modifier = Modifier.size(13.dp))
                        Text("Tutorial", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Presets Trigger
                Surface(
                    onClick = { showPresetSheet = true },
                    shape = RoundedCornerShape(12.dp),
                    color = StudioSurfaceWhite,
                    modifier = Modifier.shadow(2.dp, RoundedCornerShape(12.dp), spotColor = Color(0x0F0F172A))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(13.dp))
                        Text("Presets", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // Framerate Trigger
                Surface(
                    onClick = { showFpsSheet = true },
                    shape = RoundedCornerShape(12.dp),
                    color = StudioSurfaceWhite,
                    modifier = Modifier.shadow(2.dp, RoundedCornerShape(12.dp), spotColor = Color(0x0F0F172A))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = StudioPrimary, modifier = Modifier.size(13.dp))
                        Text("${framerateSettings.fps} FPS", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // Model Switcher Trigger
                Surface(
                    onClick = { showModelSheet = true },
                    shape = RoundedCornerShape(12.dp),
                    color = StudioSurfaceWhite,
                    modifier = Modifier.shadow(2.dp, RoundedCornerShape(12.dp), spotColor = Color(0x0F0F172A))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.ViewInAr, contentDescription = null, tint = StudioAccent, modifier = Modifier.size(13.dp))
                        Text("Model", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // Export Video Trigger
                Button(
                    onClick = { showExportDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("export_animation_header_button")
                ) {
                    Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Export", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Center 3D Viewport with Live Animation & FlipaClip 3D Onion Skinning
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
        ) {
            Viewport3D(
                mesh = currentMesh,
                modelTransform = currentTransform,
                onionSkins = onionSkins,
                modifier = Modifier.fillMaxSize()
            )
        }

        // FlipaClip-Style 3D Timeline Bar & Filmstrip Dock
        TimelineBar(
            track = track,
            currentTimeSec = currentTimeSec,
            isPlaying = isPlaying,
            selectedKeyframe = selectedKeyframe,
            framerateSettings = framerateSettings,
            isOnionSkinEnabled = isOnionSkinEnabled,
            onToggleOnionSkin = { isOnionSkinEnabled = !isOnionSkinEnabled },
            onSeek = { time ->
                currentTimeSec = time
            },
            onTogglePlay = { isPlaying = !isPlaying },
            onAddKeyframe = {
                val newKf = Keyframe(
                    timeSec = currentTimeSec,
                    posX = currentTransform.posX,
                    posY = currentTransform.posY,
                    posZ = currentTransform.posZ,
                    rotX = currentTransform.rotXDeg,
                    rotY = currentTransform.rotYDeg,
                    rotZ = currentTransform.rotZDeg,
                    scale = currentTransform.scaleX,
                    squashStretch = currentTransform.squashStretch,
                    twistDeg = currentTransform.twistDeg,
                    bendX = currentTransform.bendX,
                    easing = EasingType.SMOOTH
                )
                track = track.addOrUpdateKeyframe(newKf)
                selectedKeyframe = newKf
            },
            onDuplicateKeyframe = { id ->
                val newTime = (currentTimeSec + 0.5f).coerceAtMost(track.durationSec)
                track = track.duplicateKeyframe(sourceId = id, newTimeSec = newTime)
                selectedKeyframe = track.keyframes.find { kotlin.math.abs(it.timeSec - newTime) < 0.05f }
                currentTimeSec = newTime
            },
            onDeleteKeyframe = { id ->
                track = track.removeKeyframe(id)
                selectedKeyframe = track.keyframes.firstOrNull()
            },
            onSelectKeyframe = { kf ->
                selectedKeyframe = kf
                currentTimeSec = kf.timeSec
            },
            onEditPose = {
                if (selectedKeyframe == null && track.keyframes.isNotEmpty()) {
                    selectedKeyframe = track.keyframes.first()
                }
                showPoseInspector = true
            },
            onStepPrevFrame = {
                isPlaying = false
                currentTimeSec = framerateSettings.prevFrameTime(currentTimeSec)
            },
            onStepNextFrame = {
                isPlaying = false
                currentTimeSec = framerateSettings.nextFrameTime(currentTimeSec, track.durationSec)
            },
            onToggleLoop = {
                track = track.copy(isLooping = !track.isLooping)
            }
        )
    }

    // Tucked Menu 1: Movement Presets Bottom Sheet
    if (showPresetSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPresetSheet = false },
            containerColor = Color.White,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(20.dp))
                        Text("3D Movement Presets", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    IconButton(onClick = { showPresetSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextTertiary)
                    }
                }

                Text(
                    text = "Apply 1-tap choreographed camera and mesh motion paths to your 3D model:",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(MovementPreset.values()) { preset ->
                        val isSelected = activePreset == preset
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) StudioAccentMuted else StudioSurfaceMuted,
                            modifier = Modifier
                                .width(170.dp)
                                .shadow(if (isSelected) 2.dp else 0.dp, RoundedCornerShape(12.dp), spotColor = Color(0x0F0F172A))
                                .clickable {
                                    activePreset = preset
                                    track = track.withPreset(preset)
                                    selectedKeyframe = track.keyframes.firstOrNull()
                                    currentTimeSec = 0f
                                    isPlaying = true
                                    showPresetSheet = false
                                }
                                .testTag("preset_${preset.name}")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    preset.title,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    preset.description,
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    maxLines = 2
                                )
                                Text(
                                    "${String.format(Locale.US, "%.1f", preset.defaultDurationSec)}s duration",
                                    color = StudioAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Tucked Menu 2: Framerate & Timing Sheet
    if (showFpsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFpsSheet = false },
            containerColor = Color.White,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = StudioPrimary, modifier = Modifier.size(20.dp))
                        Text("Customizable Framerate", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    IconButton(onClick = { showFpsSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextTertiary)
                    }
                }

                FramerateSelector(
                    settings = framerateSettings,
                    animationDurationSec = track.durationSec,
                    onFpsChanged = { newFps ->
                        framerateSettings = FramerateSettings(newFps)
                    }
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Tucked Menu 3: 3D Model Library Switcher
    if (showModelSheet) {
        ModalBottomSheet(
            onDismissRequest = { showModelSheet = false },
            containerColor = Color.White,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.ViewInAr, contentDescription = null, tint = StudioAccent, modifier = Modifier.size(20.dp))
                        Text("Switch Active 3D Mesh", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    IconButton(onClick = { showModelSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextTertiary)
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(modelLibrary) { model ->
                        val isCurrent = model.id == currentMesh.id
                        Surface(
                            onClick = {
                                onSelectMesh(model)
                                showModelSheet = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) StudioAccentMuted else StudioSurfaceMuted,
                            modifier = Modifier
                                .width(150.dp)
                                .shadow(if (isCurrent) 2.dp else 0.dp, RoundedCornerShape(12.dp), spotColor = Color(0x0F0F172A))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(model.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${model.polygonCount} Polys", color = StudioAccent, fontSize = 11.sp)
                                Text("${model.vertexCount} Verts", color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Tucked Menu 4: FlipaClip 3D Pose & Deformation Inspector Sheet
    if (showPoseInspector && selectedKeyframe != null) {
        val kf = selectedKeyframe!!
        var inspectorTab by remember { mutableIntStateOf(0) } // 0: Deformation, 1: Transform, 2: Easing

        ModalBottomSheet(
            onDismissRequest = { showPoseInspector = false },
            containerColor = Color.White,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = StudioPrimary, modifier = Modifier.size(20.dp))
                        Text(
                            "Frame Pose @ ${String.format(Locale.US, "%.2f", kf.timeSec)}s",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    IconButton(onClick = { showPoseInspector = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextTertiary)
                    }
                }

                // Inspector Tabs: Squash & Stretch, Transform, Easing
                TabRow(
                    selectedTabIndex = inspectorTab,
                    containerColor = Color(0xFFF1F5F9),
                    contentColor = StudioPrimary,
                    indicator = { tabPositions ->
                        SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[inspectorTab]),
                            color = StudioPrimary
                        )
                    }
                ) {
                    Tab(
                        selected = inspectorTab == 0,
                        onClick = { inspectorTab = 0 },
                        text = { Text("Squash & Bend", fontSize = 12.sp, color = if (inspectorTab == 0) StudioPrimary else TextSecondary) }
                    )
                    Tab(
                        selected = inspectorTab == 1,
                        onClick = { inspectorTab = 1 },
                        text = { Text("Transform", fontSize = 12.sp, color = if (inspectorTab == 1) StudioPrimary else TextSecondary) }
                    )
                    Tab(
                        selected = inspectorTab == 2,
                        onClick = { inspectorTab = 2 },
                        text = { Text("Easing", fontSize = 12.sp, color = if (inspectorTab == 2) StudioPrimary else TextSecondary) }
                    )
                }

                when (inspectorTab) {
                    // TAB 0: FlipaClip 3D Deformation Physics (Squash & Stretch, Twist, Bend)
                    0 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Squash & Stretch Slider
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Squash & Stretch (Volume Preserved)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        when {
                                            kf.squashStretch < -0.05f -> "Squash: ${String.format(Locale.US, "%.2f", kf.squashStretch)}"
                                            kf.squashStretch > 0.05f -> "Stretch: +${String.format(Locale.US, "%.2f", kf.squashStretch)}"
                                            else -> "Neutral (0.0)"
                                        },
                                        color = StudioAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Slider(
                                    value = kf.squashStretch,
                                    onValueChange = { newVal ->
                                        val updated = kf.copy(squashStretch = newVal)
                                        track = track.addOrUpdateKeyframe(updated)
                                        selectedKeyframe = updated
                                    },
                                    valueRange = -0.75f..1.25f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = StudioPrimary,
                                        activeTrackColor = StudioPrimary,
                                        inactiveTrackColor = Color(0xFFE2E8F0)
                                    )
                                )
                            }

                            // Twist Slider
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Twist Deformation", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("${kf.twistDeg.toInt()}°", color = StudioAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = kf.twistDeg,
                                    onValueChange = { newVal ->
                                        val updated = kf.copy(twistDeg = newVal)
                                        track = track.addOrUpdateKeyframe(updated)
                                        selectedKeyframe = updated
                                    },
                                    valueRange = -180f..180f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = StudioAmber,
                                        activeTrackColor = StudioAmber,
                                        inactiveTrackColor = Color(0xFFE2E8F0)
                                    )
                                )
                            }

                            // Bend Slider
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Bend Curvature", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(String.format(Locale.US, "%.2f", kf.bendX), color = StudioAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = kf.bendX,
                                    onValueChange = { newVal ->
                                        val updated = kf.copy(bendX = newVal)
                                        track = track.addOrUpdateKeyframe(updated)
                                        selectedKeyframe = updated
                                    },
                                    valueRange = -0.8f..0.8f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = StudioAccent,
                                        activeTrackColor = StudioAccent,
                                        inactiveTrackColor = Color(0xFFE2E8F0)
                                    )
                                )
                            }
                        }
                    }

                    // TAB 1: Transform (Rotation, Height, Scale)
                    1 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Rotation Yaw (Y)
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Rotation (Yaw Y)", color = TextPrimary, fontSize = 12.sp)
                                    Text("${kf.rotY.toInt()}°", color = StudioPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = kf.rotY,
                                    onValueChange = { newRot ->
                                        val updated = kf.copy(rotY = newRot)
                                        track = track.addOrUpdateKeyframe(updated)
                                        selectedKeyframe = updated
                                    },
                                    valueRange = -360f..360f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = StudioPrimary,
                                        activeTrackColor = StudioPrimary,
                                        inactiveTrackColor = Color(0xFFE2E8F0)
                                    )
                                )
                            }

                            // Height Elevation (Y)
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Elevation (Height Y)", color = TextPrimary, fontSize = 12.sp)
                                    Text(String.format(Locale.US, "%.2f", kf.posY), color = StudioPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = kf.posY,
                                    onValueChange = { newY ->
                                        val updated = kf.copy(posY = newY)
                                        track = track.addOrUpdateKeyframe(updated)
                                        selectedKeyframe = updated
                                    },
                                    valueRange = -1.5f..1.5f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = StudioPrimary,
                                        activeTrackColor = StudioPrimary,
                                        inactiveTrackColor = Color(0xFFE2E8F0)
                                    )
                                )
                            }

                            // Scale
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Scale", color = TextPrimary, fontSize = 12.sp)
                                    Text(String.format(Locale.US, "%.2fx", kf.scale), color = StudioPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = kf.scale,
                                    onValueChange = { newScale ->
                                        val updated = kf.copy(scale = newScale)
                                        track = track.addOrUpdateKeyframe(updated)
                                        selectedKeyframe = updated
                                    },
                                    valueRange = 0.2f..2.5f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = StudioPrimary,
                                        activeTrackColor = StudioPrimary,
                                        inactiveTrackColor = Color(0xFFE2E8F0)
                                    )
                                )
                            }
                        }
                    }

                    // TAB 2: Interpolation & Easing
                    2 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Frame Transition Easing:", color = TextSecondary, fontSize = 12.sp)
                            EasingType.values().forEach { easing ->
                                val isChosen = kf.easing == easing
                                Surface(
                                    onClick = {
                                        val updated = kf.copy(easing = easing)
                                        track = track.addOrUpdateKeyframe(updated)
                                        selectedKeyframe = updated
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isChosen) StudioAccentMuted else StudioSurfaceMuted,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(if (isChosen) 2.dp else 0.dp, RoundedCornerShape(10.dp), spotColor = Color(0x0F0F172A))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = easing.name.replace("_", " "),
                                                color = if (isChosen) StudioAccent else TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = when (easing) {
                                                    EasingType.SMOOTH -> "Standard natural ease-in-out curve"
                                                    EasingType.STEP -> "Stop-motion / FlipaClip hard frame switch"
                                                    EasingType.BOUNCE -> "Physical collision bounce & impact"
                                                    EasingType.ELASTIC -> "Snappy rubberized snap-back"
                                                    EasingType.ANTICIPATION -> "Classic cartoon backward wind-up"
                                                    EasingType.LINEAR -> "Constant speed mechanical motion"
                                                    else -> "Standard easing curve"
                                                },
                                                color = TextSecondary,
                                                fontSize = 10.sp
                                            )
                                        }
                                        if (isChosen) {
                                            Text("ACTIVE", color = StudioAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showExportDialog) {
        ExportDialog(
            mesh = currentMesh,
            track = track,
            framerateSettings = framerateSettings,
            onDismiss = { showExportDialog = false }
        )
    }
}

