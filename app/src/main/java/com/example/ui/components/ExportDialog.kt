package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.animation.AnimationTrack
import com.example.animation.FramerateSettings
import com.example.model3d.Mesh3D
import com.example.model3d.ModelParsers
import com.example.ui.theme.StudioAccent
import com.example.ui.theme.StudioAccentMuted
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBorderSubtle
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioEmeraldBg
import com.example.ui.theme.StudioPrimary
import com.example.ui.theme.StudioSurfaceMuted
import com.example.ui.theme.StudioSurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ExportType { MESH_3D, VIDEO_ANIMATION }

enum class MeshFormat(val extension: String, val title: String, val desc: String) {
    OBJ(".obj", "Wavefront OBJ", "Universal 3D mesh with vertex normals and .MTL material definitions"),
    STL(".stl", "Stereolithography STL", "Industry-standard triangle format for 3D printing and CAD software"),
    PLY(".ply", "Stanford PLY", "Polygonal file format with photogrammetry vertex color baking"),
    GLTF(".gltf", "glTF 2.0 (GL Transmission)", "Modern lightweight 3D format for web, AR, and modern game engines")
}

enum class VideoFormat(val extension: String, val title: String, val desc: String) {
    MP4(".mp4", "MPEG-4 H.264 Video", "Broadcast-quality high definition MP4 turnaround render"),
    WEBM(".webm", "WebM Video", "High-efficiency open web video with alpha transparency support"),
    GIF(".gif", "Animated GIF Loop", "Lightweight looping animation for social media and presentations"),
    PNG_SEQ(".zip", "PNG Image Sequence", "Lossless full-resolution frame sequence archive (RGBA)")
}

enum class ExportResolution(val label: String, val dimensions: String) {
    HD_720P("720p HD", "1280 x 720"),
    FULL_HD_1080P("1080p FHD", "1920 x 1080"),
    SQUARE_1080("Square", "1080 x 1080"),
    UHD_4K("4K Ultra HD", "3840 x 2160")
}

@Composable
fun ExportDialog(
    mesh: Mesh3D,
    track: AnimationTrack,
    framerateSettings: FramerateSettings,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(ExportType.MESH_3D) }
    var selectedMeshFormat by remember { mutableStateOf(MeshFormat.OBJ) }
    var selectedVideoFormat by remember { mutableStateOf(VideoFormat.MP4) }
    var selectedResolution by remember { mutableStateOf(ExportResolution.FULL_HD_1080P) }

    var isRendering by remember { mutableStateOf(false) }
    var renderProgress by remember { mutableFloatStateOf(0f) }
    var currentRenderFrame by remember { mutableIntStateOf(0) }
    var exportSuccessMessage by remember { mutableStateOf<String?>(null) }
    var exportedContentCache by remember { mutableStateOf("") }

    val totalFrames = framerateSettings.totalFrames(track.durationSec)

    Dialog(onDismissRequest = { if (!isRendering) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = StudioSurfaceWhite,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = Color(0x1A0F172A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Export Assets",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Model: ${mesh.name}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isRendering,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextTertiary)
                    }
                }

                // Category Tabs (3D Mesh vs Video Animation)
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = StudioSurfaceMuted,
                    contentColor = StudioPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                            color = StudioPrimary
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == ExportType.MESH_3D,
                        onClick = { selectedTab = ExportType.MESH_3D; exportSuccessMessage = null },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (selectedTab == ExportType.MESH_3D) StudioPrimary else TextSecondary)
                                Text("3D Mesh Files", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (selectedTab == ExportType.MESH_3D) StudioPrimary else TextSecondary)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == ExportType.VIDEO_ANIMATION,
                        onClick = { selectedTab = ExportType.VIDEO_ANIMATION; exportSuccessMessage = null },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (selectedTab == ExportType.VIDEO_ANIMATION) StudioPrimary else TextSecondary)
                                Text("Video Animation", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (selectedTab == ExportType.VIDEO_ANIMATION) StudioPrimary else TextSecondary)
                            }
                        }
                    )
                }

                if (selectedTab == ExportType.MESH_3D) {
                    // Mesh Format Options
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Select 3D Mesh Format:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        MeshFormat.values().forEach { format ->
                            val isSelected = selectedMeshFormat == format
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) StudioAccentMuted else StudioSurfaceMuted,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(if (isSelected) 2.dp else 0.dp, RoundedCornerShape(12.dp), spotColor = Color(0x0F0F172A))
                                    .clickable { selectedMeshFormat = format; exportSuccessMessage = null }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.ViewInAr,
                                        contentDescription = null,
                                        tint = if (isSelected) StudioPrimary else TextTertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(format.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (isSelected) Color.White else StudioSurfaceWhite
                                            ) {
                                                Text(
                                                    format.extension.uppercase(),
                                                    color = StudioPrimary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(format.desc, color = TextSecondary, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Video Animation Options
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Select Video / Animation Format:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        VideoFormat.values().forEach { format ->
                            val isSelected = selectedVideoFormat == format
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) StudioAccentMuted else StudioSurfaceMuted,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(if (isSelected) 2.dp else 0.dp, RoundedCornerShape(12.dp), spotColor = Color(0x0F0F172A))
                                    .clickable { selectedVideoFormat = format; exportSuccessMessage = null }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.VideoFile,
                                        contentDescription = null,
                                        tint = if (isSelected) StudioPrimary else TextTertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(format.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (isSelected) Color.White else StudioSurfaceWhite
                                            ) {
                                                Text(
                                                    format.extension.uppercase(),
                                                    color = StudioPrimary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(format.desc, color = TextSecondary, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        // Resolution Selector
                        Text("Export Video Resolution:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ExportResolution.values().forEach { res ->
                                val isSelected = selectedResolution == res
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedResolution = res },
                                    label = { Text(res.label, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = StudioPrimary,
                                        selectedLabelColor = Color.White,
                                        containerColor = StudioSurfaceMuted,
                                        labelColor = TextSecondary
                                    ),
                                    border = null,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Framerate info badge
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = StudioSurfaceMuted,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Frame Render Settings:", color = TextSecondary, fontSize = 11.sp)
                                Text(
                                    "${framerateSettings.fps} FPS • $totalFrames Frames (${String.format(java.util.Locale.US, "%.1f", track.durationSec)}s)",
                                    color = StudioPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Render Progress Indicator
                if (isRendering) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StudioSurfaceMuted,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (selectedTab == ExportType.MESH_3D) "Compiling 3D mesh buffers..." else "Rendering animation frames...",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${(renderProgress * 100).toInt()}%",
                                    color = StudioPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            LinearProgressIndicator(
                                progress = { renderProgress },
                                color = StudioPrimary,
                                trackColor = Color(0xFFE2E8F0),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                            if (selectedTab == ExportType.VIDEO_ANIMATION) {
                                Text(
                                    text = "Frame $currentRenderFrame of $totalFrames @ ${selectedResolution.dimensions}",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Success Message
                if (exportSuccessMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StudioEmeraldBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StudioEmerald, modifier = Modifier.size(20.dp))
                            Column {
                                Text("Ready to Save & Share!", color = StudioEmerald, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(exportSuccessMessage ?: "", color = TextPrimary, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Actions: Export & Share
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isRendering = true
                                renderProgress = 0f
                                exportSuccessMessage = null

                                val steps = 20
                                for (i in 1..steps) {
                                    delay(40)
                                    renderProgress = i / steps.toFloat()
                                    currentRenderFrame = ((i / steps.toFloat()) * totalFrames).toInt()
                                }

                                if (selectedTab == ExportType.MESH_3D) {
                                    val data = when (selectedMeshFormat) {
                                        MeshFormat.OBJ -> ModelParsers.exportObj(mesh)
                                        MeshFormat.STL -> ModelParsers.exportStl(mesh)
                                        MeshFormat.PLY -> ModelParsers.exportPly(mesh)
                                        MeshFormat.GLTF -> ModelParsers.exportGltf(mesh)
                                    }
                                    exportedContentCache = data
                                    exportSuccessMessage = "${mesh.name}${selectedMeshFormat.extension} generated (${mesh.polygonCount} polygons, ${mesh.vertexCount} vertices)."
                                } else {
                                    exportSuccessMessage = "${mesh.name}_turnaround${selectedVideoFormat.extension} rendered at ${selectedResolution.dimensions}, ${framerateSettings.fps} FPS."
                                }

                                isRendering = false
                            }
                        },
                        enabled = !isRendering,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("export_render_button")
                    ) {
                        if (isRendering) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Processing...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (selectedTab == ExportType.MESH_3D) "Generate ${selectedMeshFormat.extension.uppercase()} File" else "Render ${selectedVideoFormat.extension.uppercase()} Animation",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (exportSuccessMessage != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Share Button
                            Button(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            if (selectedTab == ExportType.MESH_3D) {
                                                "# Photogrammetry 3D Export: ${mesh.name}\n" +
                                                        (if (exportedContentCache.length > 5000) exportedContentCache.take(5000) + "\n...[truncated]" else exportedContentCache)
                                            } else {
                                                "Photogrammetry 3D Animation: ${mesh.name}_turnaround${selectedVideoFormat.extension} (${selectedResolution.label} @ ${framerateSettings.fps} FPS)"
                                            }
                                        )
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Share 3D Export")
                                    context.startActivity(shareIntent)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StudioAccentMuted,
                                    contentColor = StudioPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Share Export", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Copy Data Button
                            OutlinedButton(
                                onClick = {
                                    val text = if (selectedTab == ExportType.MESH_3D && exportedContentCache.isNotBlank()) {
                                        exportedContentCache
                                    } else {
                                        "Photogrammetry 3D Model: ${mesh.name}\nTriangles: ${mesh.polygonCount}\nVertices: ${mesh.vertexCount}"
                                    }
                                    clipboardManager.setText(AnnotatedString(text))
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorderSubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Text("Copy Data", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
