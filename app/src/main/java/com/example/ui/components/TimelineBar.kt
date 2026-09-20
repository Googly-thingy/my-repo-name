package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animation.AnimationTrack
import com.example.animation.FramerateSettings
import com.example.animation.Keyframe
import com.example.ui.theme.StudioAccent
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioPrimary
import com.example.ui.theme.StudioSurfaceMuted
import com.example.ui.theme.StudioSurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.util.Locale

@Composable
fun TimelineBar(
    track: AnimationTrack,
    currentTimeSec: Float,
    isPlaying: Boolean,
    selectedKeyframe: Keyframe?,
    framerateSettings: FramerateSettings,
    isOnionSkinEnabled: Boolean = true,
    onToggleOnionSkin: () -> Unit = {},
    onSeek: (Float) -> Unit,
    onTogglePlay: () -> Unit,
    onAddKeyframe: () -> Unit,
    onDuplicateKeyframe: (String) -> Unit = {},
    onDeleteKeyframe: (String) -> Unit,
    onSelectKeyframe: (Keyframe) -> Unit,
    onEditPose: () -> Unit = {},
    onStepPrevFrame: () -> Unit,
    onStepNextFrame: () -> Unit,
    onToggleLoop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = Color(0x150F172A))
            .clip(RoundedCornerShape(20.dp)),
        color = StudioSurfaceWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Timecode, Onion Skin Toggle, and Edit Pose button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timecode Readout
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val currentFrame = framerateSettings.timeToFrameIndex(currentTimeSec)
                    val totalFrames = framerateSettings.totalFrames(track.durationSec)
                    Text(
                        text = String.format(Locale.US, "%.2fs / %.2fs", currentTimeSec, track.durationSec),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "• F#$currentFrame/$totalFrames",
                        color = StudioAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Quick Mode Toggles
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // FlipaClip 3D Onion Skin Toggle Button
                    Surface(
                        onClick = onToggleOnionSkin,
                        shape = RoundedCornerShape(20.dp),
                        color = if (isOnionSkinEnabled) Color(0xFFEFF6FF) else StudioSurfaceMuted
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "🧅", fontSize = 11.sp)
                            Text(
                                text = if (isOnionSkinEnabled) "Skin ON" else "Skin OFF",
                                color = if (isOnionSkinEnabled) StudioAccent else TextTertiary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Edit 3D Pose / Morph Button
                    Button(
                        onClick = onEditPose,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 11.dp, vertical = 5.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Edit Pose", modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Edit Pose", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Interactive Scrubber Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF1F5F9))
                    .pointerInput(track.durationSec) {
                        detectTapGestures { offset ->
                            val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                            val tappedTime = ratio * track.durationSec
                            onSeek(tappedTime)

                            val hit = track.keyframes.find { kf ->
                                val kfRatio = kf.timeSec / track.durationSec
                                val kfX = kfRatio * size.width
                                kotlin.math.abs(kfX - offset.x) < 24f
                            }
                            if (hit != null) {
                                onSelectKeyframe(hit)
                            }
                        }
                    }
                    .pointerInput(track.durationSec) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val ratio = (change.position.x / size.width).coerceIn(0f, 1f)
                            onSeek(ratio * track.durationSec)
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(44.dp)) {
                    val w = size.width
                    val h = size.height

                    val secCount = track.durationSec.toInt()
                    for (s in 0..secCount) {
                        val x = (s / track.durationSec) * w
                        drawLine(
                            color = Color(0xFFCBD5E1),
                            start = Offset(x, 0f),
                            end = Offset(x, h * 0.35f),
                            strokeWidth = 1.0f
                        )
                        for (sub in 1..3) {
                            val subSec = s + sub * 0.25f
                            if (subSec < track.durationSec) {
                                val subX = (subSec / track.durationSec) * w
                                drawLine(
                                    color = Color(0xFFE2E8F0),
                                    start = Offset(subX, 0f),
                                    end = Offset(subX, h * 0.18f),
                                    strokeWidth = 0.8f
                                )
                            }
                        }
                    }

                    val centerY = h * 0.65f
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(0f, centerY),
                        end = Offset(w, centerY),
                        strokeWidth = 2.5f
                    )

                    // Draw Keyframe Diamonds
                    for (kf in track.keyframes) {
                        val kfX = (kf.timeSec / track.durationSec).coerceIn(0f, 1f) * w
                        val isSelected = selectedKeyframe?.id == kf.id
                        val diamondSize = if (isSelected) 8f else 6f
                        val diamondColor = if (isSelected) Color(0xFF2563EB) else Color(0xFF64748B)

                        val diamondPath = Path().apply {
                            moveTo(kfX, centerY - diamondSize)
                            lineTo(kfX + diamondSize, centerY)
                            lineTo(kfX, centerY + diamondSize)
                            lineTo(kfX - diamondSize, centerY)
                            close()
                        }
                        drawPath(diamondPath, color = diamondColor)
                    }

                    // Draw Current Playhead Line
                    val playheadX = (currentTimeSec / track.durationSec).coerceIn(0f, 1f) * w
                    drawLine(
                        color = Color(0xFF0F172A),
                        start = Offset(playheadX, 0f),
                        end = Offset(playheadX, h),
                        strokeWidth = 2.0f
                    )
                    drawCircle(
                        color = Color(0xFF0F172A),
                        radius = 4.5f,
                        center = Offset(playheadX, 6f)
                    )
                }
            }

            // FlipaClip-Style Horizontal Frame Filmstrip
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "3D KEYFRAME FILMSTRIP",
                        color = TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    if (selectedKeyframe != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Clone / Duplicate pose
                            Surface(
                                onClick = { onDuplicateKeyframe(selectedKeyframe.id) },
                                shape = RoundedCornerShape(8.dp),
                                color = StudioSurfaceMuted
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = StudioAccent, modifier = Modifier.size(11.dp))
                                    Text("Duplicate", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                }
                            }

                            // Delete frame
                            Surface(
                                onClick = { onDeleteKeyframe(selectedKeyframe.id) },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFEE2E2)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(11.dp))
                                    Text("Delete", color = Color(0xFFB91C1C), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                // Horizontal list of Frame Cards
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemsIndexed(track.keyframes) { index, kf ->
                        val isSelected = selectedKeyframe?.id == kf.id
                        Surface(
                            onClick = {
                                onSelectKeyframe(kf)
                                onSeek(kf.timeSec)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, StudioAccent) else null,
                            shadowElevation = if (isSelected) 2.dp else 0.dp,
                            modifier = Modifier.size(width = 74.dp, height = 54.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "#${index + 1}",
                                        color = if (isSelected) StudioAccent else TextTertiary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                    if (kf.squashStretch != 0f) {
                                        Text(text = "⚡", fontSize = 9.sp)
                                    }
                                }
                                Text(
                                    text = String.format(Locale.US, "%.2fs", kf.timeSec),
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = kf.easing.name.take(4),
                                    color = TextTertiary,
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }

                    // Add Frame Card at end
                    item {
                        Surface(
                            onClick = onAddKeyframe,
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.size(width = 54.dp, height = 54.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add frame", tint = StudioPrimary, modifier = Modifier.size(20.dp))
                                Text("Frame", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Transport Controls: Prev Frame, Play/Pause, Next Frame, Loop
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleLoop,
                    modifier = Modifier
                        .size(36.dp)
                        .background(if (track.isLooping) Color(0xFFEFF6FF) else Color.Transparent, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Toggle Loop",
                        tint = if (track.isLooping) StudioAccent else TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onSeek(0f) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.FastRewind, contentDescription = "Rewind to Start", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = onStepPrevFrame,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Step Back Frame", tint = TextPrimary, modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(4.dp, CircleShape)
                            .background(StudioPrimary, CircleShape)
                            .testTag("play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = onStepNextFrame,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Step Forward Frame", tint = TextPrimary, modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = { onSeek(track.durationSec) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.FastForward, contentDescription = "Fast Forward", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StudioSurfaceMuted
                ) {
                    Text(
                        text = "${track.keyframes.size} Frames",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}


