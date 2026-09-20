package com.example.model3d

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class RenderMode(val label: String) {
    SHADED("Textured & Shaded"),
    WIREFRAME("Wireframe"),
    POINT_CLOUD("Point Cloud"),
    CLAY_STUDIO("Clay Studio")
}

data class CameraTransform(
    val yawDeg: Float = 35f,
    val pitchDeg: Float = 20f,
    val distance: Float = 3.2f,
    val panX: Float = 0f,
    val panY: Float = 0f
)

data class ModelTransform(
    val posX: Float = 0f,
    val posY: Float = 0f,
    val posZ: Float = 0f,
    val rotXDeg: Float = 0f,
    val rotYDeg: Float = 0f,
    val rotZDeg: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val scaleZ: Float = 1f,
    // FlipaClip-style 3D Morph & Deformation Layer
    val squashStretch: Float = 0f, // -0.8 (squash) to +1.5 (stretch)
    val twistDeg: Float = 0f,      // -180 to +180 deg
    val bendX: Float = 0f          // -1.0 to +1.0
)

object Renderer3D {

    private val LIGHT_DIR = Vector3(0.5f, 0.8f, -0.6f).normalized()
    private val LIGHT_DIR_SECONDARY = Vector3(-0.4f, -0.3f, 0.7f).normalized()

    private class ProjectedPoint(
        val screenX: Float,
        val screenY: Float,
        val depth: Float
    )

    private class ProjectedTriangle(
        val p1: ProjectedPoint,
        val p2: ProjectedPoint,
        val p3: ProjectedPoint,
        val avgDepth: Float,
        val shadedColor: Color,
        val wireColor: Color
    )

    fun render(
        drawScope: DrawScope,
        mesh: Mesh3D,
        camera: CameraTransform,
        modelTransform: ModelTransform = ModelTransform(),
        renderMode: RenderMode = RenderMode.SHADED,
        showFloorGrid: Boolean = true,
        onionSkins: List<Pair<ModelTransform, Color>> = emptyList()
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        if (width <= 0f || height <= 0f || mesh.vertices.isEmpty()) return

        val centerX = width * 0.5f + camera.panX
        val centerY = height * 0.5f + camera.panY
        val focalLength = min(width, height) * 0.95f

        val yawRad = Math.toRadians(camera.yawDeg.toDouble()).toFloat()
        val pitchRad = Math.toRadians(camera.pitchDeg.toDouble()).toFloat()

        val cosYaw = cos(yawRad)
        val sinYaw = sin(yawRad)
        val cosPitch = cos(pitchRad)
        val sinPitch = sin(pitchRad)

        // Draw Floor Grid
        if (showFloorGrid) {
            drawFloorGrid(drawScope, centerX, centerY, focalLength, camera, cosYaw, sinYaw, cosPitch, sinPitch)
        }

        // Render FlipaClip 3D Onion Skins (Ghosted Previous & Next Frame Wireframes)
        for ((ghostTransform, ghostColor) in onionSkins) {
            renderMeshPass(
                drawScope = drawScope,
                mesh = mesh,
                modelTransform = ghostTransform,
                camera = camera,
                centerX = centerX,
                centerY = centerY,
                focalLength = focalLength,
                cosYaw = cosYaw,
                sinYaw = sinYaw,
                cosPitch = cosPitch,
                sinPitch = sinPitch,
                renderMode = RenderMode.WIREFRAME,
                overrideWireColor = ghostColor,
                wireStrokeWidth = 1.0f
            )
        }

        // Render Primary Active Mesh
        renderMeshPass(
            drawScope = drawScope,
            mesh = mesh,
            modelTransform = modelTransform,
            camera = camera,
            centerX = centerX,
            centerY = centerY,
            focalLength = focalLength,
            cosYaw = cosYaw,
            sinYaw = sinYaw,
            cosPitch = cosPitch,
            sinPitch = sinPitch,
            renderMode = renderMode
        )
    }

    private fun renderMeshPass(
        drawScope: DrawScope,
        mesh: Mesh3D,
        modelTransform: ModelTransform,
        camera: CameraTransform,
        centerX: Float,
        centerY: Float,
        focalLength: Float,
        cosYaw: Float,
        sinYaw: Float,
        cosPitch: Float,
        sinPitch: Float,
        renderMode: RenderMode,
        overrideWireColor: Color? = null,
        wireStrokeWidth: Float = 1.2f
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height

        val projectedVertices = ArrayList<ProjectedPoint?>(mesh.vertices.size)
        val cameraSpaceVertices = ArrayList<Vector3>(mesh.vertices.size)

        for (v in mesh.vertices) {
            var mx = v.x
            var my = v.y
            var mz = v.z

            // 1. Squash & Stretch Deformation (FlipaClip cartoon physics)
            if (modelTransform.squashStretch != 0f) {
                val s = modelTransform.squashStretch.coerceIn(-0.85f, 2.2f)
                val sy = 1f + s
                val sxz = 1f / kotlin.math.sqrt(sy.coerceAtLeast(0.08f))
                my *= sy
                mx *= sxz
                mz *= sxz
            }

            // 2. Twist Deformation around Y
            if (modelTransform.twistDeg != 0f) {
                val normY = (my + 1f) * 0.5f
                val twistRad = Math.toRadians((modelTransform.twistDeg * normY).toDouble()).toFloat()
                val ct = cos(twistRad)
                val st = sin(twistRad)
                val rx = mx * ct - mz * st
                val rz = mx * st + mz * ct
                mx = rx
                mz = rz
            }

            // 3. Bend Deformation along X
            if (modelTransform.bendX != 0f) {
                val bend = modelTransform.bendX * (my * my) * 0.35f
                mx += bend
            }

            // 4. Model Scale
            mx *= modelTransform.scaleX
            my *= modelTransform.scaleY
            mz *= modelTransform.scaleZ

            // 5. Model Rotation (Euler X, Y, Z)
            if (modelTransform.rotXDeg != 0f || modelTransform.rotYDeg != 0f || modelTransform.rotZDeg != 0f) {
                val rotV = Vector3(mx, my, mz).rotateEuler(
                    modelTransform.rotXDeg,
                    modelTransform.rotYDeg,
                    modelTransform.rotZDeg
                )
                mx = rotV.x
                my = rotV.y
                mz = rotV.z
            }

            // 6. Model Translation
            val wx = mx + modelTransform.posX
            val wy = my + modelTransform.posY
            val wz = mz + modelTransform.posZ

            // 7. Camera Orbit Rotation
            val x1 = wx * cosYaw + wz * sinYaw
            val y1 = wy
            val z1 = -wx * sinYaw + wz * cosYaw

            val camX = x1
            val camY = y1 * cosPitch - z1 * sinPitch
            val camZ = y1 * sinPitch + z1 * cosPitch + camera.distance

            cameraSpaceVertices.add(Vector3(camX, camY, camZ))

            if (camZ > 0.15f) {
                val projX = centerX + (camX / camZ) * focalLength
                val projY = centerY - (camY / camZ) * focalLength
                projectedVertices.add(ProjectedPoint(projX, projY, camZ))
            } else {
                projectedVertices.add(null)
            }
        }

        // Point Cloud mode
        if (renderMode == RenderMode.POINT_CLOUD) {
            for (pt in projectedVertices) {
                if (pt != null && pt.screenX in -50f..(width + 50f) && pt.screenY in -50f..(height + 50f)) {
                    val radius = max(1.5f, min(5f, 10f / pt.depth))
                    drawScope.drawCircle(
                        color = overrideWireColor ?: Color(0xFF00E5FF),
                        radius = radius,
                        center = Offset(pt.screenX, pt.screenY)
                    )
                }
            }
            return
        }

        // Project and depth-sort triangles
        val projectedTriangles = ArrayList<ProjectedTriangle>(mesh.triangles.size)

        for (t in mesh.triangles) {
            val p1 = projectedVertices.getOrNull(t.v1) ?: continue
            val p2 = projectedVertices.getOrNull(t.v2) ?: continue
            val p3 = projectedVertices.getOrNull(t.v3) ?: continue

            val cross = (p2.screenX - p1.screenX) * (p3.screenY - p1.screenY) -
                    (p2.screenY - p1.screenY) * (p3.screenX - p1.screenX)

            if (cross > 0f) {
                val avgDepth = (p1.depth + p2.depth + p3.depth) / 3f

                val rotatedNormal = if (modelTransform.rotXDeg != 0f || modelTransform.rotYDeg != 0f || modelTransform.rotZDeg != 0f) {
                    t.normal.rotateEuler(modelTransform.rotXDeg, modelTransform.rotYDeg, modelTransform.rotZDeg)
                } else t.normal

                val baseColor = when (renderMode) {
                    RenderMode.CLAY_STUDIO -> Color(0xFFD6D3D1)
                    else -> t.color
                }

                val diffuse = max(0.0f, rotatedNormal.dot(LIGHT_DIR))
                val bounce = max(0.0f, rotatedNormal.dot(LIGHT_DIR_SECONDARY)) * 0.25f
                val ambient = 0.35f
                val intensity = min(1.0f, ambient + diffuse * 0.55f + bounce)

                val shadedColor = Color(
                    red = (baseColor.red * intensity).coerceIn(0f, 1f),
                    green = (baseColor.green * intensity).coerceIn(0f, 1f),
                    blue = (baseColor.blue * intensity).coerceIn(0f, 1f),
                    alpha = 1.0f
                )

                val wireColor = overrideWireColor ?: when (renderMode) {
                    RenderMode.WIREFRAME -> Color(0xFF1E293B)
                    else -> Color(
                        red = (shadedColor.red * 0.75f).coerceIn(0f, 1f),
                        green = (shadedColor.green * 0.75f).coerceIn(0f, 1f),
                        blue = (shadedColor.blue * 0.75f).coerceIn(0f, 1f),
                        alpha = 0.4f
                    )
                }

                projectedTriangles.add(ProjectedTriangle(p1, p2, p3, avgDepth, shadedColor, wireColor))
            }
        }

        projectedTriangles.sortByDescending { it.avgDepth }

        val path = Path()
        for (tri in projectedTriangles) {
            path.reset()
            path.moveTo(tri.p1.screenX, tri.p1.screenY)
            path.lineTo(tri.p2.screenX, tri.p2.screenY)
            path.lineTo(tri.p3.screenX, tri.p3.screenY)
            path.close()

            if (renderMode != RenderMode.WIREFRAME && overrideWireColor == null) {
                drawScope.drawPath(
                    path = path,
                    color = tri.shadedColor,
                    style = Fill
                )
            }

            if (renderMode == RenderMode.WIREFRAME || overrideWireColor != null) {
                drawScope.drawPath(
                    path = path,
                    color = tri.wireColor,
                    style = Stroke(width = wireStrokeWidth)
                )
            }
        }
    }

    private fun drawFloorGrid(
        drawScope: DrawScope,
        centerX: Float,
        centerY: Float,
        focalLength: Float,
        camera: CameraTransform,
        cosYaw: Float,
        sinYaw: Float,
        cosPitch: Float,
        sinPitch: Float
    ) {
        val gridY = -1.05f // Ground floor position
        val gridSize = 1.8f
        val steps = 8
        val stepSize = (gridSize * 2f) / steps

        fun projectFloor(gx: Float, gz: Float): Offset? {
            val x1 = gx * cosYaw + gz * sinYaw
            val y1 = gridY
            val z1 = -gx * sinYaw + gz * cosYaw

            val camX = x1
            val camY = y1 * cosPitch - z1 * sinPitch
            val camZ = y1 * sinPitch + z1 * cosPitch + camera.distance

            if (camZ <= 0.1f) return null
            val sx = centerX + (camX / camZ) * focalLength
            val sy = centerY - (camY / camZ) * focalLength
            return Offset(sx, sy)
        }

        val gridColor = Color(0x1F0F172A)
        val centerAxisColor = Color(0x402563EB)

        // Grid lines along X and Z
        for (i in 0..steps) {
            val coord = -gridSize + i * stepSize

            // Line parallel to Z
            val pStart1 = projectFloor(coord, -gridSize)
            val pEnd1 = projectFloor(coord, gridSize)
            if (pStart1 != null && pEnd1 != null) {
                val col = if (i == steps / 2) centerAxisColor else gridColor
                drawScope.drawLine(col, pStart1, pEnd1, strokeWidth = if (i == steps / 2) 1.5f else 0.8f)
            }

            // Line parallel to X
            val pStart2 = projectFloor(-gridSize, coord)
            val pEnd2 = projectFloor(gridSize, coord)
            if (pStart2 != null && pEnd2 != null) {
                val col = if (i == steps / 2) centerAxisColor else gridColor
                drawScope.drawLine(col, pStart2, pEnd2, strokeWidth = if (i == steps / 2) 1.5f else 0.8f)
            }
        }

        // Circular turntable orbit ring on floor
        val ringSegments = 32
        var prevOffset: Offset? = null
        val ringRadius = 1.4f
        for (s in 0..ringSegments) {
            val a = (s.toFloat() / ringSegments) * 2f * Math.PI.toFloat()
            val rx = ringRadius * cos(a)
            val rz = ringRadius * sin(a)
            val curOffset = projectFloor(rx, rz)
            if (prevOffset != null && curOffset != null) {
                drawScope.drawLine(Color(0x302563EB), prevOffset, curOffset, strokeWidth = 1.0f)
            }
            prevOffset = curOffset
        }
    }
}
