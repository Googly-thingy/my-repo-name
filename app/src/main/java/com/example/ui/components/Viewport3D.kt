package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model3d.CameraTransform
import com.example.model3d.Mesh3D
import com.example.model3d.ModelTransform
import com.example.model3d.RenderMode
import com.example.model3d.Renderer3D
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioSurface

@Composable
fun Viewport3D(
    mesh: Mesh3D,
    modifier: Modifier = Modifier,
    modelTransform: ModelTransform = ModelTransform(),
    initialCamera: CameraTransform = CameraTransform(),
    allowUserOrbit: Boolean = true,
    overlayContent: @Composable () -> Unit = {}
) {
    var camera by remember { mutableStateOf(initialCamera) }
    var renderMode by remember { mutableStateOf(RenderMode.SHADED) }
    var showGrid by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        StudioDarkBg,
                        StudioSurface,
                        Color(0xFF0D131F)
                    )
                )
            )
            .border(1.dp, Color(0xFF374151), RoundedCornerShape(16.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("viewport_3d_canvas")
                .pointerInput(allowUserOrbit) {
                    if (!allowUserOrbit) return@pointerInput
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val newDist = (camera.distance / zoom).coerceIn(0.8f, 12f)
                        if (zoom != 1f) {
                            camera = camera.copy(distance = newDist)
                        } else {
                            val newYaw = (camera.yawDeg + pan.x * 0.45f) % 360f
                            val newPitch = (camera.pitchDeg + pan.y * 0.45f).coerceIn(-85f, 85f)
                            camera = camera.copy(yawDeg = newYaw, pitchDeg = newPitch)
                        }
                    }
                }
        ) {
            Renderer3D.render(
                drawScope = this,
                mesh = mesh,
                camera = camera,
                modelTransform = modelTransform,
                renderMode = renderMode,
                showFloorGrid = showGrid
            )
        }

        // Top HUD Bar: Mesh Information & Render Modes
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mesh Stats Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StudioSurface.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF374151))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = "Mesh info",
                            tint = StudioCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${mesh.polygonCount} Polys • ${mesh.vertexCount} Verts",
                            color = Color(0xFFE5E7EB),
                            fontSize = 11.sp
                        )
                    }
                }

                // Quick View Actions
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Grid Toggle
                    IconButton(
                        onClick = { showGrid = !showGrid },
                        modifier = Modifier
                            .size(34.dp)
                            .background(StudioSurface.copy(alpha = 0.85f), CircleShape)
                            .border(1.dp, Color(0xFF374151), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = "Toggle Grid",
                            tint = if (showGrid) StudioCyan else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Reset Camera
                    IconButton(
                        onClick = { camera = initialCamera },
                        modifier = Modifier
                            .size(34.dp)
                            .background(StudioSurface.copy(alpha = 0.85f), CircleShape)
                            .border(1.dp, Color(0xFF374151), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Camera View",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Mode Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RenderMode.values().forEach { mode ->
                    val isSelected = renderMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { renderMode = mode },
                        label = {
                            Text(
                                text = mode.label,
                                fontSize = 10.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudioCyan.copy(alpha = 0.2f),
                            selectedLabelColor = StudioCyan,
                            containerColor = StudioSurface.copy(alpha = 0.8f),
                            labelColor = Color(0xFF9CA3AF)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) StudioCyan else Color(0xFF374151)
                        ),
                        modifier = Modifier.size(height = 28.dp, width = androidx.compose.ui.unit.Dp.Unspecified)
                    )
                }
            }
        }

        // Custom Overlay Content (e.g. photogrammetry orbit guidance or animation badge)
        Box(modifier = Modifier.fillMaxSize()) {
            overlayContent()
        }
    }
}
