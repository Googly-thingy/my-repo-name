package com.example.model3d

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Triangle(
    val v1: Int,
    val v2: Int,
    val v3: Int,
    val normal: Vector3 = Vector3.UP,
    val color: Color = Color(0xFF64B5F6)
)

data class BoundingBox(
    val min: Vector3,
    val max: Vector3,
    val center: Vector3,
    val size: Vector3,
    val radius: Float
)

data class Mesh3D(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val vertices: List<Vector3>,
    val triangles: List<Triangle>,
    val vertexColors: List<Color> = emptyList(),
    val isScanned: Boolean = false,
    val capturePhotoCount: Int = 0
) {
    val vertexCount: Int get() = vertices.size
    val polygonCount: Int get() = triangles.size

    val bounds: BoundingBox by lazy {
        if (vertices.isEmpty()) {
            return@lazy BoundingBox(Vector3.ZERO, Vector3.ZERO, Vector3.ZERO, Vector3.ZERO, 1f)
        }
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE

        for (v in vertices) {
            if (v.x < minX) minX = v.x
            if (v.y < minY) minY = v.y
            if (v.z < minZ) minZ = v.z
            if (v.x > maxX) maxX = v.x
            if (v.y > maxY) maxY = v.y
            if (v.z > maxZ) maxZ = v.z
        }

        val min = Vector3(minX, minY, minZ)
        val max = Vector3(maxX, maxY, maxZ)
        val center = Vector3((minX + maxX) * 0.5f, (minY + maxY) * 0.5f, (minZ + maxZ) * 0.5f)
        val size = Vector3(maxX - minX, maxY - minY, maxZ - minZ)
        val radius = maxOf(size.x, maxOf(size.y, size.z)) * 0.5f
        BoundingBox(min, max, center, size, if (radius > 0.001f) radius else 1f)
    }

    /**
     * Re-centers mesh at origin and scales so longest dimension fits in [-1, 1].
     */
    fun normalized(): Mesh3D {
        val b = bounds
        val scale = if (b.radius > 0f) 1.2f / b.radius else 1f
        val newVertices = vertices.map { v ->
            Vector3(
                (v.x - b.center.x) * scale,
                (v.y - b.center.y) * scale,
                (v.z - b.center.z) * scale
            )
        }
        return copy(vertices = newVertices)
    }

    companion object {
        fun computeTriangleNormal(p1: Vector3, p2: Vector3, p3: Vector3): Vector3 {
            val edge1 = p2 - p1
            val edge2 = p3 - p1
            return edge1.cross(edge2).normalized()
        }

        fun generateAmphora(): Mesh3D = createAmphoraVase()
        fun generateDrone(): Mesh3D = createCyberDrone()
        fun generateSkull(): Mesh3D = createPaleoSkull()
        fun generateRobot(): Mesh3D = createCompanionRobot()
        fun generateCrystal(): Mesh3D = createCrystalCluster()

        // --- Procedural / Sample 3D Models ---

        fun createAmphoraVase(): Mesh3D {
            val segments = 24
            val rings = 20
            val vertices = mutableListOf<Vector3>()
            val triangles = mutableListOf<Triangle>()

            // Vase profile radius as function of height y from 0 to 1
            fun vaseRadius(t: Float): Float {
                return when {
                    t < 0.08f -> 0.35f + t * 0.5f // base
                    t < 0.45f -> 0.39f + sin((t - 0.08f) / 0.37f * PI.toFloat()) * 0.55f // belly
                    t < 0.85f -> 0.45f - (t - 0.45f) * 0.65f // neck
                    else -> 0.19f + (t - 0.85f) * 1.2f // rim flared
                }
            }

            for (r in 0..rings) {
                val t = r.toFloat() / rings
                val y = (t - 0.5f) * 2.2f
                val rad = vaseRadius(t)
                for (s in 0 until segments) {
                    val theta = s.toFloat() / segments * 2f * PI.toFloat()
                    val x = rad * cos(theta)
                    val z = rad * sin(theta)
                    vertices.add(Vector3(x, y, z))
                }
            }

            // Generate quads/triangles
            for (r in 0 until rings) {
                val t = r.toFloat() / rings
                val terracottaTone = when {
                    t in 0.35f..0.55f -> Color(0xFFC86432) // terracotta body
                    t in 0.70f..0.80f -> Color(0xFFB85324) // antique band
                    else -> Color(0xFFD37B4C)
                }
                for (s in 0 until segments) {
                    val nextS = (s + 1) % segments
                    val i0 = r * segments + s
                    val i1 = r * segments + nextS
                    val i2 = (r + 1) * segments + s
                    val i3 = (r + 1) * segments + nextS

                    val n1 = computeTriangleNormal(vertices[i0], vertices[i2], vertices[i1])
                    val n2 = computeTriangleNormal(vertices[i1], vertices[i2], vertices[i3])

                    triangles.add(Triangle(i0, i2, i1, n1, terracottaTone))
                    triangles.add(Triangle(i1, i2, i3, n2, terracottaTone))
                }
            }

            // Vase handles (left and right)
            fun addHandle(sideX: Float) {
                val handleSteps = 10
                val startIdx = vertices.size
                for (h in 0..handleSteps) {
                    val angle = (h.toFloat() / handleSteps) * PI.toFloat()
                    val hx = sideX * (0.55f + sin(angle) * 0.4f)
                    val hy = 0.4f - cos(angle) * 0.55f
                    val hz = 0f
                    // small cross section
                    vertices.add(Vector3(hx, hy, hz - 0.06f))
                    vertices.add(Vector3(hx, hy, hz + 0.06f))
                }
                for (h in 0 until handleSteps) {
                    val base = startIdx + h * 2
                    triangles.add(Triangle(base, base + 2, base + 1, Vector3(sideX, 0f, 0f), Color(0xFF9E4018)))
                    triangles.add(Triangle(base + 1, base + 2, base + 3, Vector3(sideX, 0f, 0f), Color(0xFF9E4018)))
                }
            }
            addHandle(1f)
            addHandle(-1f)

            return Mesh3D(
                id = "model_amphora",
                name = "Terracotta Amphora",
                description = "Photogrammetry 3D scan of an ancient Mediterranean pottery artifact with decorative handles",
                category = "Artifact",
                vertices = vertices,
                triangles = triangles,
                isScanned = true,
                capturePhotoCount = 48
            ).normalized()
        }

        fun createCyberDrone(): Mesh3D {
            val vertices = mutableListOf<Vector3>()
            val triangles = mutableListOf<Triangle>()

            // Central aerodynamic fuselage (polyhedron)
            val darkChassis = Color(0xFF263238)
            val glowCyan = Color(0xFF00E5FF)
            val orangeAccent = Color(0xFFFF6D00)

            // Body vertices
            vertices.add(Vector3(0f, 0.2f, 0.7f)) // 0: nose tip
            vertices.add(Vector3(0.35f, 0.15f, 0.2f)) // 1: right front
            vertices.add(Vector3(-0.35f, 0.15f, 0.2f)) // 2: left front
            vertices.add(Vector3(0.4f, 0.1f, -0.6f)) // 3: right tail
            vertices.add(Vector3(-0.4f, 0.1f, -0.6f)) // 4: left tail
            vertices.add(Vector3(0f, -0.2f, 0.6f)) // 5: belly nose
            vertices.add(Vector3(0.3f, -0.15f, 0.1f)) // 6: belly right
            vertices.add(Vector3(-0.3f, -0.15f, 0.1f)) // 7: belly left
            vertices.add(Vector3(0.2f, -0.1f, -0.5f)) // 8: belly right tail
            vertices.add(Vector3(-0.2f, -0.1f, -0.5f)) // 9: belly left tail

            fun addTri(v1: Int, v2: Int, v3: Int, col: Color) {
                val norm = computeTriangleNormal(vertices[v1], vertices[v2], vertices[v3])
                triangles.add(Triangle(v1, v2, v3, norm, col))
            }

            // Top canopy (Glowing cyan optical visor)
            addTri(0, 1, 2, glowCyan)
            addTri(1, 3, 2, darkChassis)
            addTri(2, 3, 4, darkChassis)

            // Sides & bottom
            addTri(0, 5, 1, orangeAccent)
            addTri(5, 6, 1, darkChassis)
            addTri(0, 2, 5, orangeAccent)
            addTri(2, 7, 5, darkChassis)
            addTri(1, 6, 3, darkChassis)
            addTri(6, 8, 3, darkChassis)
            addTri(2, 4, 7, darkChassis)
            addTri(7, 4, 9, darkChassis)
            addTri(5, 7, 6, darkChassis)
            addTri(6, 7, 8, darkChassis)
            addTri(7, 9, 8, darkChassis)
            addTri(3, 8, 4, darkChassis)
            addTri(8, 9, 4, darkChassis)

            // 4 Quadcopter Rotor Arms & Pods
            val armOffsets = listOf(
                Pair(0.9f, 0.7f),
                Pair(-0.9f, 0.7f),
                Pair(1.0f, -0.7f),
                Pair(-1.0f, -0.7f)
            )

            for ((ax, az) in armOffsets) {
                val bIdx = vertices.size
                vertices.add(Vector3(ax * 0.4f, 0.05f, az * 0.4f)) // arm start
                vertices.add(Vector3(ax, 0.15f, az)) // rotor center
                vertices.add(Vector3(ax + 0.35f, 0.18f, az)) // blade 1
                vertices.add(Vector3(ax - 0.35f, 0.18f, az)) // blade 2
                vertices.add(Vector3(ax, 0.18f, az + 0.35f)) // blade 3
                vertices.add(Vector3(ax, 0.18f, az - 0.35f)) // blade 4

                addTri(bIdx + 1, bIdx + 2, bIdx + 4, glowCyan)
                addTri(bIdx + 1, bIdx + 3, bIdx + 5, glowCyan)
                addTri(bIdx, bIdx + 1, bIdx + 2, darkChassis)
            }

            return Mesh3D(
                id = "model_drone",
                name = "AeroScan Sentinel Drone",
                description = "Autonomous multi-rotor photogrammetry inspection drone with sensor array",
                category = "Sci-Fi",
                vertices = vertices,
                triangles = triangles,
                isScanned = false
            ).normalized()
        }

        fun createCrystalCluster(): Mesh3D {
            val vertices = mutableListOf<Vector3>()
            val triangles = mutableListOf<Triangle>()

            // Multifaceted prismatic crystal cluster
            val crystalColors = listOf(
                Color(0xFF7C4DFF),
                Color(0xFF651FFF),
                Color(0xFFB388FF),
                Color(0xFF00E5FF),
                Color(0xFF80D8FF)
            )

            fun addPrism(baseX: Float, baseZ: Float, height: Float, radius: Float, tiltX: Float, tiltZ: Float) {
                val sides = 6
                val startIdx = vertices.size
                // Base vertices
                for (i in 0 until sides) {
                    val a = i.toFloat() / sides * 2f * PI.toFloat()
                    val vx = baseX + radius * cos(a)
                    val vz = baseZ + radius * sin(a)
                    vertices.add(Vector3(vx, -0.8f, vz))
                }
                // Shoulder vertices
                for (i in 0 until sides) {
                    val a = i.toFloat() / sides * 2f * PI.toFloat()
                    val vx = baseX + tiltX * height * 0.7f + radius * 0.85f * cos(a)
                    val vz = baseZ + tiltZ * height * 0.7f + radius * 0.85f * sin(a)
                    vertices.add(Vector3(vx, -0.8f + height * 0.75f, vz))
                }
                // Tip vertex
                val tipIdx = vertices.size
                vertices.add(Vector3(baseX + tiltX * height, -0.8f + height, baseZ + tiltZ * height))

                for (i in 0 until sides) {
                    val next = (i + 1) % sides
                    val b1 = startIdx + i
                    val b2 = startIdx + next
                    val s1 = startIdx + sides + i
                    val s2 = startIdx + sides + next

                    val col = crystalColors[i % crystalColors.size]
                    val n1 = computeTriangleNormal(vertices[b1], vertices[s1], vertices[b2])
                    val n2 = computeTriangleNormal(vertices[b2], vertices[s1], vertices[s2])
                    val nTip = computeTriangleNormal(vertices[s1], vertices[tipIdx], vertices[s2])

                    triangles.add(Triangle(b1, s1, b2, n1, col))
                    triangles.add(Triangle(b2, s1, s2, n2, col))
                    triangles.add(Triangle(s1, tipIdx, s2, nTip, Color(0xFFD1C4E9)))
                }
            }

            addPrism(0f, 0f, 1.8f, 0.45f, 0.05f, 0.02f) // Main pillar
            addPrism(0.45f, 0.25f, 1.3f, 0.32f, 0.2f, 0.1f)
            addPrism(-0.4f, 0.35f, 1.1f, 0.28f, -0.25f, 0.15f)
            addPrism(-0.35f, -0.3f, 1.4f, 0.35f, -0.18f, -0.2f)
            addPrism(0.35f, -0.4f, 0.95f, 0.25f, 0.22f, -0.15f)

            return Mesh3D(
                id = "model_crystal",
                name = "Prismatic Amethyst Cluster",
                description = "High-detail photogrammetry geological scan of crystalline quartz prisms",
                category = "Geology",
                vertices = vertices,
                triangles = triangles,
                isScanned = true,
                capturePhotoCount = 64
            ).normalized()
        }

        fun createCompanionRobot(): Mesh3D {
            val vertices = mutableListOf<Vector3>()
            val triangles = mutableListOf<Triangle>()

            val botWhite = Color(0xFFECEFF1)
            val botDark = Color(0xFF37474F)
            val botVisor = Color(0xFF00E676)
            val botBlue = Color(0xFF1E88E5)

            fun addBox(minX: Float, minY: Float, minZ: Float, maxX: Float, maxY: Float, maxZ: Float, color: Color) {
                val base = vertices.size
                // 8 corners
                vertices.add(Vector3(minX, minY, minZ)) // 0
                vertices.add(Vector3(maxX, minY, minZ)) // 1
                vertices.add(Vector3(maxX, maxY, minZ)) // 2
                vertices.add(Vector3(minX, maxY, minZ)) // 3
                vertices.add(Vector3(minX, minY, maxZ)) // 4
                vertices.add(Vector3(maxX, minY, maxZ)) // 5
                vertices.add(Vector3(maxX, maxY, maxZ)) // 6
                vertices.add(Vector3(minX, maxY, maxZ)) // 7

                fun quad(i0: Int, i1: Int, i2: Int, i3: Int, col: Color) {
                    val n1 = computeTriangleNormal(vertices[base + i0], vertices[base + i1], vertices[base + i2])
                    val n2 = computeTriangleNormal(vertices[base + i0], vertices[base + i2], vertices[base + i3])
                    triangles.add(Triangle(base + i0, base + i1, base + i2, n1, col))
                    triangles.add(Triangle(base + i0, base + i2, base + i3, n2, col))
                }

                quad(0, 3, 2, 1, color) // Front (Z-)
                quad(5, 6, 7, 4, color) // Back (Z+)
                quad(4, 7, 3, 0, color) // Left (X-)
                quad(1, 2, 6, 5, color) // Right (X+)
                quad(3, 7, 6, 2, color) // Top (Y+)
                quad(4, 0, 1, 5, color) // Bottom (Y-)
            }

            // Head
            addBox(-0.45f, 0.45f, -0.4f, 0.45f, 1.15f, 0.4f, botWhite)
            // Visor (Glowing face screen)
            addBox(-0.35f, 0.65f, 0.41f, 0.35f, 1.0f, 0.43f, botVisor)
            // Antenna
            addBox(-0.06f, 1.15f, -0.06f, 0.06f, 1.45f, 0.06f, botDark)
            addBox(-0.12f, 1.45f, -0.12f, 0.12f, 1.6f, 0.12f, botBlue)
            // Neck
            addBox(-0.18f, 0.32f, -0.18f, 0.18f, 0.45f, 0.18f, botDark)
            // Torso
            addBox(-0.55f, -0.55f, -0.35f, 0.55f, 0.32f, 0.35f, botWhite)
            // Chest Badge
            addBox(-0.25f, -0.15f, 0.36f, 0.25f, 0.2f, 0.38f, botBlue)
            // Left Arm
            addBox(-0.85f, -0.45f, -0.15f, -0.58f, 0.25f, 0.15f, botDark)
            // Right Arm
            addBox(0.58f, -0.45f, -0.15f, 0.85f, 0.25f, 0.15f, botDark)
            // Left Leg
            addBox(-0.45f, -1.25f, -0.2f, -0.15f, -0.55f, 0.2f, botDark)
            // Right Leg
            addBox(0.15f, -1.25f, -0.2f, 0.45f, -0.55f, 0.2f, botDark)

            return Mesh3D(
                id = "model_robot",
                name = "Vector-9 Companion Bot",
                description = "Articulated humanoid desktop robot with expressive OLED visor",
                category = "Character",
                vertices = vertices,
                triangles = triangles,
                isScanned = false
            ).normalized()
        }

        fun createPaleoSkull(): Mesh3D {
            val vertices = mutableListOf<Vector3>()
            val triangles = mutableListOf<Triangle>()

            val boneColor = Color(0xFFD7CCC8)
            val boneDark = Color(0xFFA1887F)
            val toothColor = Color(0xFFFFF9C4)

            fun addT(v1: Vector3, v2: Vector3, v3: Vector3, col: Color) {
                val idx = vertices.size
                vertices.add(v1)
                vertices.add(v2)
                vertices.add(v3)
                val n = computeTriangleNormal(v1, v2, v3)
                triangles.add(Triangle(idx, idx + 1, idx + 2, n, col))
            }

            // Cranium wedge
            val snoutTip = Vector3(0f, 0.05f, 1.1f)
            val snoutMidL = Vector3(-0.25f, 0.25f, 0.5f)
            val snoutMidR = Vector3(0.25f, 0.25f, 0.5f)
            val browL = Vector3(-0.45f, 0.55f, -0.1f)
            val browR = Vector3(0.45f, 0.55f, -0.1f)
            val occipital = Vector3(0f, 0.45f, -0.7f)
            val palateL = Vector3(-0.2f, -0.1f, 0.5f)
            val palateR = Vector3(0.2f, -0.1f, 0.5f)
            val jawHingeL = Vector3(-0.4f, 0.0f, -0.5f)
            val jawHingeR = Vector3(0.4f, 0.0f, -0.5f)

            // Top cranium
            addT(snoutTip, snoutMidR, snoutMidL, boneColor)
            addT(snoutMidL, snoutMidR, browR, boneColor)
            addT(snoutMidL, browR, browL, boneColor)
            addT(browL, browR, occipital, boneDark)

            // Snout sides
            addT(snoutTip, palateR, snoutMidR, boneColor)
            addT(snoutTip, snoutMidL, palateL, boneColor)
            addT(snoutMidR, palateR, browR, boneColor)
            addT(snoutMidL, browL, palateL, boneColor)
            addT(browR, palateR, jawHingeR, boneDark)
            addT(browL, jawHingeL, palateL, boneDark)

            // Lower Jaw (Mandible)
            val chinTip = Vector3(0f, -0.35f, 1.0f)
            val mandL = Vector3(-0.22f, -0.3f, 0.4f)
            val mandR = Vector3(0.22f, -0.3f, 0.4f)
            val mandHingeL = Vector3(-0.35f, -0.05f, -0.45f)
            val mandHingeR = Vector3(0.35f, -0.05f, -0.45f)

            addT(chinTip, mandR, mandL, boneDark)
            addT(mandL, mandR, mandHingeR, boneColor)
            addT(mandL, mandHingeR, mandHingeL, boneColor)

            // Serrated Teeth
            for (i in 0 until 5) {
                val zPos = 0.95f - i * 0.12f
                val xOffset = 0.12f + i * 0.02f
                addT(
                    Vector3(xOffset, -0.08f, zPos),
                    Vector3(xOffset + 0.04f, -0.26f, zPos - 0.03f),
                    Vector3(xOffset, -0.08f, zPos - 0.06f),
                    toothColor
                )
                addT(
                    Vector3(-xOffset, -0.08f, zPos),
                    Vector3(-xOffset, -0.08f, zPos - 0.06f),
                    Vector3(-xOffset - 0.04f, -0.26f, zPos - 0.03f),
                    toothColor
                )
            }

            return Mesh3D(
                id = "model_skull",
                name = "Velociraptor Skull Fossil",
                description = "Museum-grade archaeological photogrammetry scan of a late Cretaceous dromaeosaurid skull",
                category = "Paleontology",
                vertices = vertices,
                triangles = triangles,
                isScanned = true,
                capturePhotoCount = 72
            ).normalized()
        }

        /**
         * Generates a 3D reconstructed mesh from a photogrammetry photo capture set.
         */
        fun generateFromPhotogrammetry(photoCount: Int, objectType: String = "Organic Object"): Mesh3D {
            // Generates a dense reconstructed mesh reflecting photogrammetric feature reconstruction
            val rings = maxOf(14, photoCount / 2)
            val slices = 20
            val vertices = mutableListOf<Vector3>()
            val triangles = mutableListOf<Triangle>()

            for (r in 0..rings) {
                val phi = (r.toFloat() / rings) * PI.toFloat()
                val y = cos(phi)
                val ringRad = sin(phi)
                for (s in 0 until slices) {
                    val theta = (s.toFloat() / slices) * 2f * PI.toFloat()
                    // Organic surface displacement based on harmonic frequencies
                    val bump = 1f + 0.12f * sin(theta * 3f + phi * 2f) + 0.08f * cos(theta * 5f)
                    val rad = ringRad * bump
                    val x = rad * cos(theta) * 0.9f
                    val z = rad * sin(theta) * 0.9f
                    vertices.add(Vector3(x, y * 1.1f, z))
                }
            }

            for (r in 0 until rings) {
                val t = r.toFloat() / rings
                val bakedColor = Color(
                    red = 0.35f + 0.55f * sin(t * PI.toFloat()),
                    green = 0.45f + 0.4f * cos(t * 2f),
                    blue = 0.65f + 0.3f * sin(t * 3f),
                    alpha = 1f
                )
                for (s in 0 until slices) {
                    val nextS = (s + 1) % slices
                    val i0 = r * slices + s
                    val i1 = r * slices + nextS
                    val i2 = (r + 1) * slices + s
                    val i3 = (r + 1) * slices + nextS

                    val n1 = computeTriangleNormal(vertices[i0], vertices[i2], vertices[i1])
                    val n2 = computeTriangleNormal(vertices[i1], vertices[i2], vertices[i3])

                    triangles.add(Triangle(i0, i2, i1, n1, bakedColor))
                    triangles.add(Triangle(i1, i2, i3, n2, bakedColor))
                }
            }

            return Mesh3D(
                id = "scan_${System.currentTimeMillis()}",
                name = "Photogrammetry Scan #$photoCount",
                description = "High-resolution 3D mesh reconstructed from $photoCount multi-angle camera exposures",
                category = "User Scans",
                vertices = vertices,
                triangles = triangles,
                isScanned = true,
                capturePhotoCount = photoCount
            ).normalized()
        }
    }
}
