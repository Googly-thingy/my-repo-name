package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioIndigo
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun AnimateScreen(
    currentMesh: Mesh3D,
    modelLibrary: List<Mesh3D>,
    onSelectMesh: (Mesh3D) -> Unit,
    modifier: Modifier = Modifier
) {
    var track by remember { mutableStateOf(AnimationTrack()) }
    var currentTimeSec by remember { mutableFloatStateOf(0f) }
    var isPlaying by remember { mutableStateOf(true) }
    var selectedKeyframe by remember { mutableStateOf<Keyframe?>(track.keyframes.firstOrNull()) }
    var framerateSettings by remember { mutableStateOf(FramerateSettings(60)) }
    var activePreset by remember { mutableStateOf(MovementPreset.TURNTABLE_360) }
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "3D Animation Studio",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Keyframes, presets, and high-framerate controls",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp
                )
            }

            // Export Video / Animation Button
            Button(
                onClick = { showExportDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioCyan,
                    contentColor = Color.Black
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("export_animation_header_button")
            ) {
                Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Export Video", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 3D Viewport with Live Animation Transform
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
        ) {
            Viewport3D(
                mesh = currentMesh,
                modelTransform = currentTransform,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Timeline Bar (Scrubber, Keyframes, Play/Pause, Step Frame)
        TimelineBar(
            track = track,
            currentTimeSec = currentTimeSec,
            isPlaying = isPlaying,
            selectedKeyframe = selectedKeyframe,
            framerateSettings = framerateSettings,
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
                    easing = EasingType.SMOOTH
                )
                track = track.addOrUpdateKeyframe(newKf)
                selectedKeyframe = newKf
            },
            onDeleteKeyframe = { id ->
                track = track.removeKeyframe(id)
                selectedKeyframe = track.keyframes.firstOrNull()
            },
            onSelectKeyframe = { kf ->
                selectedKeyframe = kf
                currentTimeSec = kf.timeSec
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

        // Pre-Made Movement Presets Section (User request: "pre-made movement presets. Make the animation system very easy to use and user-friendly for beginners.")
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = StudioSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF374151)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(18.dp))
                        Text("Pre-Made Movement Presets", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text("1-Tap Apply", color = StudioAmber, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(MovementPreset.values()) { preset ->
                        val isSelected = activePreset == preset
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) StudioCyan.copy(alpha = 0.15f) else StudioSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) StudioCyan else Color(0xFF374151)
                            ),
                            modifier = Modifier
                                .width(150.dp)
                                .clickable {
                                    activePreset = preset
                                    track = track.withPreset(preset)
                                    selectedKeyframe = track.keyframes.firstOrNull()
                                    currentTimeSec = 0f
                                    isPlaying = true
                                }
                                .testTag("preset_${preset.name}")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        preset.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Text(
                                    preset.description,
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 9.sp,
                                    maxLines = 2
                                )
                                Text(
                                    "${String.format(Locale.US, "%.1f", preset.defaultDurationSec)}s duration",
                                    color = StudioCyan,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Keyframe Property Editor (Fine-tune selected keyframe)
        if (selectedKeyframe != null) {
            val kf = selectedKeyframe!!
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = StudioSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioCyan.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(16.dp))
                            Text(
                                "Keyframe @ ${String.format(Locale.US, "%.2f", kf.timeSec)}s",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Easing selection chips
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            EasingType.values().forEach { easing ->
                                val isChosen = kf.easing == easing
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isChosen) StudioCyan else StudioSurfaceVariant,
                                    modifier = Modifier
                                        .clickable {
                                            val updated = kf.copy(easing = easing)
                                            track = track.addOrUpdateKeyframe(updated)
                                            selectedKeyframe = updated
                                        }
                                ) {
                                    Text(
                                        text = easing.name.take(4),
                                        color = if (isChosen) Color.Black else Color(0xFF9CA3AF),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Rotation Yaw (Y) Slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Rotation (Yaw Y)", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                            Text("${kf.rotY.toInt()}°", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = kf.rotY,
                            onValueChange = { newRot ->
                                val updated = kf.copy(rotY = newRot)
                                track = track.addOrUpdateKeyframe(updated)
                                selectedKeyframe = updated
                            },
                            valueRange = -360f..360f,
                            colors = SliderDefaults.colors(thumbColor = StudioCyan, activeTrackColor = StudioCyan)
                        )
                    }

                    // Elevation Height (Y) Slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Elevation (Height Y)", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                            Text(String.format(Locale.US, "%.2f", kf.posY), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = kf.posY,
                            onValueChange = { newY ->
                                val updated = kf.copy(posY = newY)
                                track = track.addOrUpdateKeyframe(updated)
                                selectedKeyframe = updated
                            },
                            valueRange = -1.5f..1.5f,
                            colors = SliderDefaults.colors(thumbColor = StudioCyan, activeTrackColor = StudioCyan)
                        )
                    }

                    // Scale Slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Scale", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                            Text(String.format(Locale.US, "%.2fx", kf.scale), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = kf.scale,
                            onValueChange = { newScale ->
                                val updated = kf.copy(scale = newScale)
                                track = track.addOrUpdateKeyframe(updated)
                                selectedKeyframe = updated
                            },
                            valueRange = 0.2f..2.5f,
                            colors = SliderDefaults.colors(thumbColor = StudioCyan, activeTrackColor = StudioCyan)
                        )
                    }
                }
            }
        }

        // Customizable Framerate Settings (User request: "Add very customizable framerate settings for animation")
        FramerateSelector(
            settings = framerateSettings,
            animationDurationSec = track.durationSec,
            onFpsChanged = { newFps ->
                framerateSettings = FramerateSettings(newFps)
            }
        )

        // Variety of 3D Models row for Animation (User request: "Add options to import a variety of 3d models for animation")
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = StudioSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF374151)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Model to Animate:", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("Active: ${currentMesh.name}", color = StudioCyan, fontSize = 11.sp)
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(modelLibrary) { model ->
                        val isCurrent = model.id == currentMesh.id
                        FilterChip(
                            selected = isCurrent,
                            onClick = { onSelectMesh(model) },
                            label = { Text(model.name, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudioCyan,
                                selectedLabelColor = Color.Black,
                                containerColor = StudioSurfaceVariant,
                                labelColor = Color(0xFF9CA3AF)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isCurrent,
                                borderColor = if (isCurrent) StudioCyan else Color(0xFF374151)
                            )
                        )
                    }
                }
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
