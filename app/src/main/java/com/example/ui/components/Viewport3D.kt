package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
    onionSkins: List<Pair<ModelTransform, Color>> = emptyList(),
    overlayContent: @Composable () -> Unit = {}
) {
    var camera by remember { mutableStateOf(initialCamera) }
    var renderMode by remember { mutableStateOf(RenderMode.SHADED) }
    var showGrid by remember { mutableStateOf(true) }
    var showDisplayMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = Color(0x150F172A))
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFF8FAFC),
                        Color(0xFFF1F5F9)
                    )
                )
            )
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("viewport_3d_canvas")
                .pointerInput(allowUserOrbit) {
                    if (!allowUserOrbit) return@pointerInput
                    detectTransformGestures { _, pan, zoom, _ ->
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
                showFloorGrid = showGrid,
                onionSkins = onionSkins
            )
        }

        // Minimalist Floating Top HUD: Stats pill + Tucked Display Menu Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Subtle Mesh Stats Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xF2FFFFFF),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = "Mesh info",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${mesh.polygonCount} Polys • ${mesh.vertexCount} Verts",
                        color = Color(0xFF334155),
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                }
            }

            // Tucked Display Settings Pill Button
            Box {
                Surface(
                    onClick = { showDisplayMenu = !showDisplayMenu },
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xF2FFFFFF),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Display options",
                            tint = if (showDisplayMenu) Color(0xFF2563EB) else Color(0xFF64748B),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = renderMode.label.substringBefore(" &"),
                            color = Color(0xFF334155),
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    }
                }

                // Tucked Options Dropdown Menu
                DropdownMenu(
                    expanded = showDisplayMenu,
                    onDismissRequest = { showDisplayMenu = false },
                    modifier = Modifier
                        .background(Color.White)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = "RENDER MODE",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                    RenderMode.values().forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = mode.label,
                                    color = if (renderMode == mode) Color(0xFF2563EB) else Color(0xFF1E293B),
                                    fontWeight = if (renderMode == mode) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            },
                            onClick = {
                                renderMode = mode
                                showDisplayMenu = false
                            }
                        )
                    }

                    androidx.compose.material3.HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 4.dp))

                    DropdownMenuItem(
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridOn,
                                    contentDescription = null,
                                    tint = if (showGrid) Color(0xFF2563EB) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (showGrid) "Grid: Enabled" else "Grid: Disabled",
                                    color = Color(0xFF1E293B),
                                    fontSize = 12.sp
                                )
                            }
                        },
                        onClick = {
                            showGrid = !showGrid
                            showDisplayMenu = false
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Reset Camera Angle",
                                    color = Color(0xFF1E293B),
                                    fontSize = 12.sp
                                )
                            }
                        },
                        onClick = {
                            camera = initialCamera
                            showDisplayMenu = false
                        }
                    )
                }
            }
        }

        // Custom Overlay Content
        Box(modifier = Modifier.fillMaxSize()) {
            overlayContent()
        }
    }
}
