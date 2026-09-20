package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animation.AnimationTrack
import com.example.animation.FramerateSettings
import com.example.model3d.Mesh3D
import com.example.model3d.ModelParsers
import com.example.ui.components.ExportDialog
import com.example.ui.components.Viewport3D
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioIndigo
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun StudioScreen(
    currentMesh: Mesh3D,
    modelLibrary: List<Mesh3D>,
    onSelectMesh: (Mesh3D) -> Unit,
    onImportMesh: (Mesh3D) -> Unit,
    onSaveProject: (Mesh3D) -> Unit,
    onNavigateToAnimate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showExportDialog by remember { mutableStateOf(false) }
    var saveSuccessNotification by remember { mutableStateOf(false) }

    // File import document picker for .obj, .stl, .ply
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                runCatching {
                    val content = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    }
                    if (content != null) {
                        val fileName = uri.lastPathSegment ?: "imported_model"
                        val result = when {
                            fileName.endsWith(".stl", ignoreCase = true) -> ModelParsers.parseStl(fileName, content)
                            fileName.endsWith(".ply", ignoreCase = true) -> ModelParsers.parsePly(fileName, content)
                            else -> ModelParsers.parseObj(fileName, content)
                        }
                        result.onSuccess { mesh ->
                            onImportMesh(mesh)
                            Toast.makeText(context, "Successfully imported ${mesh.name} (${mesh.polygonCount} polygons)", Toast.LENGTH_SHORT).show()
                        }.onFailure { err ->
                            Toast.makeText(context, "Failed to parse 3D file: ${err.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Studio Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "3D Mesh Studio",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Inspect, explore, and export 3D geometry",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp
                )
            }

            // Quick Import Button
            Button(
                onClick = {
                    filePickerLauncher.launch(arrayOf("*/*", "application/octet-stream", "text/plain"))
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioCyan.copy(alpha = 0.2f),
                    contentColor = StudioCyan
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioCyan),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("import_model_button")
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Import 3D", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 3D Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
        ) {
            Viewport3D(
                mesh = currentMesh,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Model Information & Actions Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = StudioSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF374151)),
            modifier = Modifier.fillMaxWidth()
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
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(currentMesh.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            if (currentMesh.isScanned) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = StudioEmerald.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioEmerald)
                                ) {
                                    Text(
                                        "SCAN",
                                        color = StudioEmerald,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(currentMesh.description, color = Color(0xFF9CA3AF), fontSize = 11.sp)
                    }

                    // Save Project Button
                    IconButton(
                        onClick = {
                            onSaveProject(currentMesh)
                            saveSuccessNotification = true
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .background(StudioSurfaceVariant, CircleShape)
                            .border(1.dp, Color(0xFF374151), CircleShape)
                            .testTag("save_project_button")
                    ) {
                        Icon(
                            imageVector = if (saveSuccessNotification) Icons.Default.Check else Icons.Default.Save,
                            contentDescription = "Save project",
                            tint = if (saveSuccessNotification) StudioEmerald else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Mesh Technical Metrics Grid
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF090D16))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Triangles", color = Color(0xFF9CA3AF), fontSize = 10.sp)
                        Text("${currentMesh.polygonCount}", color = StudioCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Vertices", color = Color(0xFF9CA3AF), fontSize = 10.sp)
                        Text("${currentMesh.vertexCount}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Bounding Span", color = Color(0xFF9CA3AF), fontSize = 10.sp)
                        Text(
                            String.format(java.util.Locale.US, "%.1f units", currentMesh.bounds.radius * 2f),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Action Buttons: Animate & Export
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onNavigateToAnimate,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioCyan,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("animate_model_button")
                    ) {
                        Icon(Icons.Default.Animation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Animate Model", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("export_mesh_dialog_button")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Export Mesh", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Variety of 3D Models Carousel (User request: "Add options to import a variety of 3d models for animation")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "3D Models Library",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "${modelLibrary.size} Available",
                    color = StudioCyan,
                    fontSize = 12.sp
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(modelLibrary) { model ->
                    val isSelected = currentMesh.id == model.id
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) StudioCyan.copy(alpha = 0.15f) else StudioSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) StudioCyan else Color(0xFF374151)
                        ),
                        modifier = Modifier
                            .width(160.dp)
                            .clickable {
                                onSelectMesh(model)
                                saveSuccessNotification = false
                            }
                            .testTag("library_item_${model.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = StudioSurfaceVariant
                                ) {
                                    Text(
                                        model.category.uppercase(),
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Active", tint = StudioCyan, modifier = Modifier.size(16.dp))
                                }
                            }

                            Text(
                                text = model.name,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                maxLines = 1
                            )

                            Text(
                                text = "${model.polygonCount} Polys",
                                color = Color(0xFF9CA3AF),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showExportDialog) {
        ExportDialog(
            mesh = currentMesh,
            track = AnimationTrack(),
            framerateSettings = FramerateSettings(60),
            onDismiss = { showExportDialog = false }
        )
    }
}
