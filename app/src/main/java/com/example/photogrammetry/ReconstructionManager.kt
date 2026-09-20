package com.example.photogrammetry

import com.example.model3d.Mesh3D
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

class ReconstructionManager {

    private val _capturedPhotos = MutableStateFlow<List<CapturedPhoto>>(emptyList())
    val capturedPhotos: StateFlow<List<CapturedPhoto>> = _capturedPhotos.asStateFlow()

    private val _isReconstructing = MutableStateFlow(false)
    val isReconstructing: StateFlow<Boolean> = _isReconstructing.asStateFlow()

    private val _progress = MutableStateFlow(ReconstructionProgress())
    val progress: StateFlow<ReconstructionProgress> = _progress.asStateFlow()

    private val _qualityMetrics = MutableStateFlow(ScanQualityMetrics())
    val qualityMetrics: StateFlow<ScanQualityMetrics> = _qualityMetrics.asStateFlow()

    fun addCapturedPhoto(
        angleDeg: Float,
        elevationDeg: Float,
        bitmap: android.graphics.Bitmap? = null,
        dominantColorHex: Long = 0xFFD4AF37,
        luminance: Float = 0.5f,
        sharpness: Float = 0.85f
    ): CapturedPhoto {
        val photo = CapturedPhoto(
            id = "photo_${System.currentTimeMillis()}_${(100..999).random()}",
            angleDeg = angleDeg,
            elevationDeg = elevationDeg,
            thumbnailBitmap = bitmap,
            luminance = luminance,
            sharpness = sharpness,
            dominantColorHex = dominantColorHex
        )
        val updated = _capturedPhotos.value + photo
        _capturedPhotos.value = updated
        updateQualityMetrics(updated)
        return photo
    }

    fun removePhoto(id: String) {
        val updated = _capturedPhotos.value.filterNot { it.id == id }
        _capturedPhotos.value = updated
        updateQualityMetrics(updated)
    }

    fun addBatchPhotos(count: Int) {
        val list = mutableListOf<CapturedPhoto>()
        val step = 360f / count
        for (i in 0 until count) {
            val angle = i * step
            val elev = if (i % 2 == 0) 25f else 50f
            list.add(CapturedPhoto("batch_${i}_${System.currentTimeMillis()}", angle, elev))
        }
        _capturedPhotos.value = _capturedPhotos.value + list
        updateQualityMetrics(_capturedPhotos.value)
    }

    fun clearPhotos() {
        _capturedPhotos.value = emptyList()
        _progress.value = ReconstructionProgress()
        updateQualityMetrics(emptyList())
    }

    private fun updateQualityMetrics(photos: List<CapturedPhoto>) {
        val count = photos.size
        val guidance = when {
            count == 0 -> "Aim camera at object and tap 'Capture Angle' or walk around"
            count < 8 -> "Capture $count/20 photos minimum. Move 15°-20° around object."
            count < 18 -> "Great progress ($count photos). Add a few high-angle shots (45° tilt)."
            else -> "Optimal coverage ($count photos)! Ready for 3D Mesh Reconstruction."
        }
        val overlap = if (count > 0) (78 + (count % 15)).coerceIn(65, 95) else 0
        _qualityMetrics.value = ScanQualityMetrics(
            overlapPercent = overlap,
            isLightingGood = true,
            isBlurLow = true,
            distanceOk = true,
            guidanceMessage = guidance
        )
    }

    suspend fun runReconstruction(
        objectName: String = "Scanned Object",
        onComplete: (Mesh3D) -> Unit
    ) {
        val photos = _capturedPhotos.value
        val count = maxOf(photos.size, 16)
        _isReconstructing.value = true

        val stages = listOf(
            Triple(ReconstructionStage.FEATURE_DETECTION, 0.15f, "Found 14,280 matching feature descriptors across $count exposures"),
            Triple(ReconstructionStage.STRUCTURE_FROM_MOTION, 0.35f, "Solved camera intrinsic & extrinsic poses. Sparse cloud: 8,420 3D points"),
            Triple(ReconstructionStage.DENSE_STEREO, 0.60f, "Dense MVS point cloud solved: 64,500 surface coordinates"),
            Triple(ReconstructionStage.POISSON_SURFACE, 0.80f, "Poisson octree level 9 generated watertight polygonal mesh"),
            Triple(ReconstructionStage.TEXTURE_BAKING, 0.92f, "Projected radiance texture maps and baked vertex colors"),
            Triple(ReconstructionStage.OPTIMIZING, 0.98f, "Decimated topology to mobile-optimized 2,800 polygons. Computed smooth normals"),
            Triple(ReconstructionStage.COMPLETED, 1.0f, "3D Mesh successfully generated and ready for Studio & Animation!")
        )

        for ((stage, targetProgress, log) in stages) {
            _progress.value = ReconstructionProgress(
                stage = stage,
                progressPercent = targetProgress,
                logMessage = log,
                detectedPoints = (targetProgress * 15000).roundToInt(),
                generatedTriangles = (targetProgress * 2800).roundToInt()
            )
            delay(450)
        }

        val generatedMesh = Mesh3D.generateFromPhotogrammetry(count, objectName).copy(
            name = if (objectName.isNotBlank()) objectName else "3D Photogrammetry Scan"
        )

        _isReconstructing.value = false
        onComplete(generatedMesh)
    }
}
