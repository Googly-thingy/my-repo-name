package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_projects")
data class SavedProject(
    @PrimaryKey
    val id: String,
    val name: String,
    val category: String,
    val createdAt: Long = System.currentTimeMillis(),
    val vertexCount: Int,
    val polygonCount: Int,
    val isScanned: Boolean,
    val capturePhotoCount: Int = 0,
    val objData: String,
    val animationPreset: String = "TURNTABLE_360",
    val fps: Int = 60,
    val durationSec: Float = 4.0f
)
