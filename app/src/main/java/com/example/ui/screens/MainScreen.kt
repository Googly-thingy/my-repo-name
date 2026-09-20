package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.SavedProject
import com.example.model3d.Mesh3D
import com.example.model3d.ModelParsers
import com.example.photogrammetry.ReconstructionManager
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioSurface
import kotlinx.coroutines.launch

enum class StudioTab(val title: String, val icon: ImageVector) {
    SCAN("3D Scan", Icons.Default.CameraAlt),
    STUDIO("3D Studio", Icons.Default.ViewInAr),
    ANIMATE("Animate", Icons.Default.Animation),
    PROJECTS("Projects", Icons.Default.Folder)
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }
    val projectDao = remember { db.projectDao() }

    val savedProjects by projectDao.getAllProjects().collectAsState(initial = emptyList())

    val reconstructionManager = remember { ReconstructionManager() }

    // Built-in starter models showcasing photogrammetry scans & variety of geometries
    val defaultModels: List<Mesh3D> = remember {
        listOf(
            Mesh3D.generateAmphora(),
            Mesh3D.generateDrone(),
            Mesh3D.generateSkull(),
            Mesh3D.generateRobot(),
            Mesh3D.generateCrystal()
        )
    }

    var modelLibrary by remember { mutableStateOf<List<Mesh3D>>(defaultModels) }
    var currentMesh by remember { mutableStateOf<Mesh3D>(defaultModels.first()) }
    var selectedTab by remember { mutableStateOf(StudioTab.STUDIO) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StudioDarkBg,
        bottomBar = {
            NavigationBar(
                containerColor = StudioSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("main_navigation_bar")
            ) {
                StudioTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = StudioCyan,
                            indicatorColor = StudioCyan,
                            unselectedIconColor = Color(0xFF9CA3AF),
                            unselectedTextColor = Color(0xFF9CA3AF)
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                StudioTab.SCAN -> {
                    ScanScreen(
                        reconstructionManager = reconstructionManager,
                        onMeshGenerated = { newMesh ->
                            currentMesh = newMesh
                            if (modelLibrary.none { model -> model.id == newMesh.id }) {
                                modelLibrary = listOf(newMesh) + modelLibrary
                            }
                            // Persist to Room
                            coroutineScope.launch {
                                val objData = ModelParsers.exportObj(newMesh)
                                projectDao.insertProject(
                                    SavedProject(
                                        id = newMesh.id,
                                        name = newMesh.name,
                                        category = "Photogrammetry Scan",
                                        vertexCount = newMesh.vertexCount,
                                        polygonCount = newMesh.polygonCount,
                                        isScanned = true,
                                        capturePhotoCount = 20,
                                        objData = objData
                                    )
                                )
                                Toast.makeText(context, "3D Model reconstructed & saved to projects!", Toast.LENGTH_SHORT).show()
                            }
                            selectedTab = StudioTab.STUDIO
                        }
                    )
                }

                StudioTab.STUDIO -> {
                    StudioScreen(
                        currentMesh = currentMesh,
                        modelLibrary = modelLibrary,
                        onSelectMesh = { currentMesh = it },
                        onImportMesh = { importedMesh ->
                            currentMesh = importedMesh
                            modelLibrary = listOf(importedMesh) + modelLibrary
                            coroutineScope.launch {
                                val objData = ModelParsers.exportObj(importedMesh)
                                projectDao.insertProject(
                                    SavedProject(
                                        id = importedMesh.id,
                                        name = importedMesh.name,
                                        category = importedMesh.category,
                                        vertexCount = importedMesh.vertexCount,
                                        polygonCount = importedMesh.polygonCount,
                                        isScanned = importedMesh.isScanned,
                                        objData = objData
                                    )
                                )
                            }
                        },
                        onSaveProject = { meshToSave ->
                            coroutineScope.launch {
                                val objData = ModelParsers.exportObj(meshToSave)
                                projectDao.insertProject(
                                    SavedProject(
                                        id = meshToSave.id,
                                        name = meshToSave.name,
                                        category = meshToSave.category,
                                        vertexCount = meshToSave.vertexCount,
                                        polygonCount = meshToSave.polygonCount,
                                        isScanned = meshToSave.isScanned,
                                        objData = objData
                                    )
                                )
                                Toast.makeText(context, "Project '${meshToSave.name}' saved!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onNavigateToAnimate = {
                            selectedTab = StudioTab.ANIMATE
                        }
                    )
                }

                StudioTab.ANIMATE -> {
                    AnimateScreen(
                        currentMesh = currentMesh,
                        modelLibrary = modelLibrary,
                        onSelectMesh = { currentMesh = it }
                    )
                }

                StudioTab.PROJECTS -> {
                    ProjectsScreen(
                        projects = savedProjects,
                        onOpenProject = { loadedMesh ->
                            currentMesh = loadedMesh
                            if (modelLibrary.none { model -> model.id == loadedMesh.id }) {
                                modelLibrary = listOf(loadedMesh) + modelLibrary
                            }
                            selectedTab = StudioTab.STUDIO
                        },
                        onDeleteProject = { id ->
                            coroutineScope.launch {
                                projectDao.deleteProjectById(id)
                                Toast.makeText(context, "Project removed", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onNavigateToScan = {
                            selectedTab = StudioTab.SCAN
                        }
                    )
                }
            }
        }
    }
}
