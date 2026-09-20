package com.example.photogrammetry

data class CapturedPhoto(
    val id: String,
    val angleDeg: Float,
    val elevationDeg: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val featurePointsCount: Int = (280..650).random(),
    val overlapPercent: Int = (75..92).random()
)

enum class ReconstructionStage(val title: String, val description: String) {
    INITIALIZING("Preparing Pipeline", "Analyzing photo dimensions and camera intrinsic matrix"),
    FEATURE_DETECTION("Feature Extraction & Matching", "Detecting Harris/ORB keypoints and matching feature descriptors"),
    STRUCTURE_FROM_MOTION("Structure-from-Motion (SfM)", "Triangulating camera poses & solving sparse 3D point cloud"),
    DENSE_STEREO("Multi-View Stereo (MVS)", "Reconstructing high-density surface point cloud"),
    POISSON_SURFACE("Poisson Surface Meshing", "Generating watertight polygonal 3D mesh topology"),
    TEXTURE_BAKING("Color & Texture Projection", "Projecting captured radiance onto 3D vertex surfaces"),
    OPTIMIZING("Mesh Decimation", "Optimizing triangle count and computing smooth surface normals"),
    COMPLETED("3D Reconstruction Complete", "High-fidelity exportable 3D model generated")
}

data class ReconstructionProgress(
    val stage: ReconstructionStage = ReconstructionStage.INITIALIZING,
    val progressPercent: Float = 0f,
    val logMessage: String = "",
    val detectedPoints: Int = 0,
    val generatedTriangles: Int = 0
)

data class ScanQualityMetrics(
    val overlapPercent: Int = 82,
    val isLightingGood: Boolean = true,
    val isBlurLow: Boolean = true,
    val distanceOk: Boolean = true,
    val guidanceMessage: String = "Good position! Keep circling around the object."
)
