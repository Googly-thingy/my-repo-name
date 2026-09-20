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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioIndigo
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import java.util.Locale

@Composable
fun TimelineBar(
    track: AnimationTrack,
    currentTimeSec: Float,
    isPlaying: Boolean,
    selectedKeyframe: Keyframe?,
    framerateSettings: FramerateSettings,
    onSeek: (Float) -> Unit,
    onTogglePlay: () -> Unit,
    onAddKeyframe: () -> Unit,
    onDeleteKeyframe: (String) -> Unit,
    onSelectKeyframe: (Keyframe) -> Unit,
    onStepPrevFrame: () -> Unit,
    onStepNextFrame: () -> Unit,
    onToggleLoop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF374151), RoundedCornerShape(16.dp)),
        color = StudioSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Timecode Readout & Add/Delete Keyframe
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timecode Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val currentFrame = framerateSettings.timeToFrameIndex(currentTimeSec)
                    val totalFrames = framerateSettings.totalFrames(track.durationSec)
                    Text(
                        text = String.format(Locale.US, "%.2fs / %.2fs", currentTimeSec, track.durationSec),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "• F#$currentFrame/$totalFrames",
                        color = StudioCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Keyframe Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (selectedKeyframe != null) {
                        FilledTonalButton(
                            onClick = { onDeleteKeyframe(selectedKeyframe.id) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF7F1D1D),
                                contentColor = Color(0xFFFECACA)
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("delete_keyframe_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Keyframe", modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Delete KF", fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = onAddKeyframe,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioCyan,
                            contentColor = Color.Black
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("add_keyframe_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Keyframe", modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("+ Keyframe", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Interactive Scrubber Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF090D16))
                    .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(8.dp))
                    .pointerInput(track.durationSec) {
                        detectTapGestures { offset ->
                            val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                            val tappedTime = ratio * track.durationSec
                            onSeek(tappedTime)

                            // Check if a keyframe was tapped
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
                Canvas(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    val w = size.width
                    val h = size.height

                    // Draw second marks and subdivisions
                    val secCount = track.durationSec.toInt()
                    for (s in 0..secCount) {
                        val x = (s / track.durationSec) * w
                        drawLine(
                            color = Color(0xFF374151),
                            start = Offset(x, 0f),
                            end = Offset(x, h * 0.4f),
                            strokeWidth = 1.2f
                        )
                        for (sub in 1..3) {
                            val subSec = s + sub * 0.25f
                            if (subSec < track.durationSec) {
                                val subX = (subSec / track.durationSec) * w
                                drawLine(
                                    color = Color(0xFF1F2937),
                                    start = Offset(subX, 0f),
                                    end = Offset(subX, h * 0.2f),
                                    strokeWidth = 0.8f
                                )
                            }
                        }
                    }

                    // Draw Timeline Track baseline
                    val centerY = h * 0.65f
                    drawLine(
                        color = Color(0xFF1F2937),
                        start = Offset(0f, centerY),
                        end = Offset(w, centerY),
                        strokeWidth = 3f
                    )

                    // Draw Keyframe Markers (Diamonds)
                    for (kf in track.keyframes) {
                        val kfX = (kf.timeSec / track.durationSec).coerceIn(0f, 1f) * w
                        val isSelected = selectedKeyframe?.id == kf.id
                        val diamondSize = if (isSelected) 8f else 6f
                        val diamondColor = if (isSelected) StudioAmber else StudioCyan

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
                        color = Color.White,
                        start = Offset(playheadX, 0f),
                        end = Offset(playheadX, h),
                        strokeWidth = 2.0f
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 5f,
                        center = Offset(playheadX, 6f)
                    )
                }
            }

            // Bottom Transport Controls: Prev Frame, Play/Pause, Next Frame, Loop
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Loop toggle
                IconButton(
                    onClick = onToggleLoop,
                    modifier = Modifier
                        .size(36.dp)
                        .background(if (track.isLooping) StudioCyan.copy(alpha = 0.2f) else Color.Transparent, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Toggle Loop",
                        tint = if (track.isLooping) StudioCyan else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Center Playback Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rewind to 0
                    IconButton(
                        onClick = { onSeek(0f) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.FastRewind, contentDescription = "Rewind to Start", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                    }

                    // Step Prev Frame
                    IconButton(
                        onClick = onStepPrevFrame,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Step Back Frame", tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    // Play/Pause Primary FAB
                    IconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier
                            .size(44.dp)
                            .background(StudioCyan, CircleShape)
                            .testTag("play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Step Next Frame
                    IconButton(
                        onClick = onStepNextFrame,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Step Forward Frame", tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    // Fast Forward to End
                    IconButton(
                        onClick = { onSeek(track.durationSec) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.FastForward, contentDescription = "Fast Forward", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                    }
                }

                // Total Keyframes count badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = StudioSurfaceVariant
                ) {
                    Text(
                        text = "${track.keyframes.size} KFs",
                        color = Color(0xFF9CA3AF),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
