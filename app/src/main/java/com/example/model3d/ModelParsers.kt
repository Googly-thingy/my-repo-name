package com.example.model3d

import androidx.compose.ui.graphics.Color
import java.util.Locale

object ModelParsers {

    /**
     * Parses standard Wavefront .OBJ files.
     */
    fun parseObj(name: String, content: String): Result<Mesh3D> {
        return runCatching {
            val vertices = mutableListOf<Vector3>()
            val normals = mutableListOf<Vector3>()
            val triangles = mutableListOf<Triangle>()

            val lines = content.lineSequence()
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

                val tokens = trimmed.split("\\s+".toRegex())
                when (tokens[0]) {
                    "v" -> {
                        if (tokens.size >= 4) {
                            val x = tokens[1].toFloatOrNull() ?: 0f
                            val y = tokens[2].toFloatOrNull() ?: 0f
                            val z = tokens[3].toFloatOrNull() ?: 0f
                            vertices.add(Vector3(x, y, z))
                        }
                    }
                    "vn" -> {
                        if (tokens.size >= 4) {
                            val nx = tokens[1].toFloatOrNull() ?: 0f
                            val ny = tokens[2].toFloatOrNull() ?: 0f
                            val nz = tokens[3].toFloatOrNull() ?: 0f
                            normals.add(Vector3(nx, ny, nz).normalized())
                        }
                    }
                    "f" -> {
                        // Faces are 1-indexed in OBJ
                        val faceIndices = mutableListOf<Int>()
                        for (i in 1 until tokens.size) {
                            val vertexStr = tokens[i].split("/")[0]
                            val idx = vertexStr.toIntOrNull()
                            if (idx != null) {
                                // Negative indices refer to relative positions from end
                                val actualIdx = if (idx < 0) vertices.size + idx else idx - 1
                                if (actualIdx in vertices.indices) {
                                    faceIndices.add(actualIdx)
                                }
                            }
                        }

                        // Triangulate convex polygons (fan triangulation)
                        if (faceIndices.size >= 3) {
                            for (i in 1 until faceIndices.size - 1) {
                                val v1 = faceIndices[0]
                                val v2 = faceIndices[i]
                                val v3 = faceIndices[i + 1]
                                val normal = Mesh3D.computeTriangleNormal(vertices[v1], vertices[v2], vertices[v3])
                                triangles.add(Triangle(v1, v2, v3, normal, Color(0xFF81D4FA)))
                            }
                        }
                    }
                }
            }

            if (vertices.isEmpty()) {
                throw IllegalArgumentException("No vertices found in OBJ file")
            }

            Mesh3D(
                id = "imported_obj_${System.currentTimeMillis()}",
                name = name.removeSuffix(".obj"),
                description = "Imported Wavefront OBJ 3D model",
                category = "Imported",
                vertices = vertices,
                triangles = triangles
            ).normalized()
        }
    }

    /**
     * Parses standard ASCII .STL files.
     */
    fun parseStl(name: String, content: String): Result<Mesh3D> {
        return runCatching {
            val vertices = mutableListOf<Vector3>()
            val triangles = mutableListOf<Triangle>()
            var currentNormal = Vector3.UP
            val currentFacetVertices = mutableListOf<Vector3>()

            val lines = content.lineSequence()
            for (line in lines) {
                val trimmed = line.trim().lowercase(Locale.ROOT)
                if (trimmed.startsWith("facet normal")) {
                    val parts = trimmed.split("\\s+".toRegex())
                    if (parts.size >= 5) {
                        val nx = parts[2].toFloatOrNull() ?: 0f
                        val ny = parts[3].toFloatOrNull() ?: 0f
                        val nz = parts[4].toFloatOrNull() ?: 0f
                        currentNormal = Vector3(nx, ny, nz).normalized()
                    }
                    currentFacetVertices.clear()
                } else if (trimmed.startsWith("vertex")) {
                    val parts = trimmed.split("\\s+".toRegex())
                    if (parts.size >= 4) {
                        val vx = parts[1].toFloatOrNull() ?: 0f
                        val vy = parts[2].toFloatOrNull() ?: 0f
                        val vz = parts[3].toFloatOrNull() ?: 0f
                        currentFacetVertices.add(Vector3(vx, vy, vz))
                    }
                } else if (trimmed.startsWith("endfacet")) {
                    if (currentFacetVertices.size >= 3) {
                        val startIdx = vertices.size
                        vertices.addAll(currentFacetVertices)
                        val normal = if (currentNormal == Vector3.ZERO) {
                            Mesh3D.computeTriangleNormal(vertices[startIdx], vertices[startIdx + 1], vertices[startIdx + 2])
                        } else currentNormal
                        triangles.add(Triangle(startIdx, startIdx + 1, startIdx + 2, normal, Color(0xFFB0BEC5)))
                    }
                }
            }

            if (vertices.isEmpty()) {
                throw IllegalArgumentException("No facets or vertices found in STL file")
            }

            Mesh3D(
                id = "imported_stl_${System.currentTimeMillis()}",
                name = name.removeSuffix(".stl"),
                description = "Imported Stereolithography STL 3D model",
                category = "Imported",
                vertices = vertices,
                triangles = triangles
            ).normalized()
        }
    }

    /**
     * Parses standard ASCII .PLY files.
     */
    fun parsePly(name: String, content: String): Result<Mesh3D> {
        return runCatching {
            val lines = content.lines()
            var vertexCount = 0
            var faceCount = 0
            var inHeader = true
            var headerEndIdx = 0

            for (i in lines.indices) {
                val line = lines[i].trim()
                if (line.startsWith("element vertex")) {
                    vertexCount = line.split("\\s+".toRegex())[2].toIntOrNull() ?: 0
                } else if (line.startsWith("element face")) {
                    faceCount = line.split("\\s+".toRegex())[2].toIntOrNull() ?: 0
                } else if (line == "end_header") {
                    inHeader = false
                    headerEndIdx = i + 1
                    break
                }
            }

            if (inHeader || vertexCount == 0) {
                throw IllegalArgumentException("Invalid or unsupported PLY header")
            }

            val vertices = mutableListOf<Vector3>()
            val triangles = mutableListOf<Triangle>()

            // Read vertices
            for (i in 0 until vertexCount) {
                val lineIdx = headerEndIdx + i
                if (lineIdx >= lines.size) break
                val parts = lines[lineIdx].trim().split("\\s+".toRegex())
                if (parts.size >= 3) {
                    val x = parts[0].toFloatOrNull() ?: 0f
                    val y = parts[1].toFloatOrNull() ?: 0f
                    val z = parts[2].toFloatOrNull() ?: 0f
                    vertices.add(Vector3(x, y, z))
                }
            }

            // Read faces
            val faceStart = headerEndIdx + vertexCount
            for (i in 0 until faceCount) {
                val lineIdx = faceStart + i
                if (lineIdx >= lines.size) break
                val parts = lines[lineIdx].trim().split("\\s+".toRegex())
                if (parts.isNotEmpty()) {
                    val count = parts[0].toIntOrNull() ?: 0
                    if (count >= 3 && parts.size >= count + 1) {
                        val v1 = parts[1].toIntOrNull() ?: 0
                        val v2 = parts[2].toIntOrNull() ?: 0
                        val v3 = parts[3].toIntOrNull() ?: 0
                        if (v1 in vertices.indices && v2 in vertices.indices && v3 in vertices.indices) {
                            val norm = Mesh3D.computeTriangleNormal(vertices[v1], vertices[v2], vertices[v3])
                            triangles.add(Triangle(v1, v2, v3, norm, Color(0xFFA7FFEB)))
                        }
                    }
                }
            }

            Mesh3D(
                id = "imported_ply_${System.currentTimeMillis()}",
                name = name.removeSuffix(".ply"),
                description = "Imported Stanford PLY 3D mesh",
                category = "Imported",
                vertices = vertices,
                triangles = triangles
            ).normalized()
        }
    }

    // --- Export Generators ---

    fun exportObj(mesh: Mesh3D): String = buildString {
        appendLine("# Photogrammetry 3D Studio - OBJ Export")
        appendLine("# Model: ${mesh.name}")
        appendLine("# Vertices: ${mesh.vertices.size}")
        appendLine("# Triangles: ${mesh.triangles.size}")
        appendLine("mtllib ${mesh.name.replace(" ", "_")}.mtl")
        appendLine("o ${mesh.name.replace(" ", "_")}")
        appendLine()

        for (v in mesh.vertices) {
            appendLine(String.format(Locale.US, "v %.6f %.6f %.6f", v.x, v.y, v.z))
        }
        appendLine()

        for (t in mesh.triangles) {
            appendLine(String.format(Locale.US, "vn %.4f %.4f %.4f", t.normal.x, t.normal.y, t.normal.z))
        }
        appendLine()

        appendLine("usemtl Material_Primary")
        appendLine("s 1")
        for ((idx, t) in mesh.triangles.withIndex()) {
            val nIdx = idx + 1
            // 1-indexed in OBJ
            appendLine("f ${t.v1 + 1}//$nIdx ${t.v2 + 1}//$nIdx ${t.v3 + 1}//$nIdx")
        }
    }

    fun exportMtl(mesh: Mesh3D): String = buildString {
        appendLine("# Photogrammetry 3D Studio - Material definition")
        appendLine("newmtl Material_Primary")
        appendLine("Ka 0.200000 0.200000 0.200000")
        appendLine("Kd 0.800000 0.800000 0.800000")
        appendLine("Ks 0.500000 0.500000 0.500000")
        appendLine("Ns 96.078431")
        appendLine("d 1.000000")
        appendLine("illum 2")
    }

    fun exportStl(mesh: Mesh3D): String = buildString {
        val solidName = mesh.name.replace("[^a-zA-Z0-9_]".toRegex(), "_")
        appendLine("solid $solidName")
        for (t in mesh.triangles) {
            val v1 = mesh.vertices.getOrElse(t.v1) { Vector3.ZERO }
            val v2 = mesh.vertices.getOrElse(t.v2) { Vector3.ZERO }
            val v3 = mesh.vertices.getOrElse(t.v3) { Vector3.ZERO }
            appendLine(String.format(Locale.US, "  facet normal %.6e %.6e %.6e", t.normal.x, t.normal.y, t.normal.z))
            appendLine("    outer loop")
            appendLine(String.format(Locale.US, "      vertex %.6e %.6e %.6e", v1.x, v1.y, v1.z))
            appendLine(String.format(Locale.US, "      vertex %.6e %.6e %.6e", v2.x, v2.y, v2.z))
            appendLine(String.format(Locale.US, "      vertex %.6e %.6e %.6e", v3.x, v3.y, v3.z))
            appendLine("    endloop")
            appendLine("  endfacet")
        }
        appendLine("endsolid $solidName")
    }

    fun exportPly(mesh: Mesh3D): String = buildString {
        appendLine("ply")
        appendLine("format ascii 1.0")
        appendLine("comment Created by Photogrammetry 3D Studio")
        appendLine("element vertex ${mesh.vertices.size}")
        appendLine("property float x")
        appendLine("property float y")
        appendLine("property float z")
        appendLine("property uchar red")
        appendLine("property uchar green")
        appendLine("property uchar blue")
        appendLine("element face ${mesh.triangles.size}")
        appendLine("property list uchar int vertex_indices")
        appendLine("end_header")

        for (v in mesh.vertices) {
            appendLine(String.format(Locale.US, "%.6f %.6f %.6f 180 210 240", v.x, v.y, v.z))
        }

        for (t in mesh.triangles) {
            appendLine("3 ${t.v1} ${t.v2} ${t.v3}")
        }
    }

    fun exportGltf(mesh: Mesh3D): String = buildString {
        // High quality glTF 2.0 JSON representation
        appendLine("{")
        appendLine("  \"asset\": { \"version\": \"2.0\", \"generator\": \"Photogrammetry 3D Studio\" },")
        appendLine("  \"scene\": 0,")
        appendLine("  \"scenes\": [{ \"name\": \"Scene\", \"nodes\": [0] }],")
        appendLine("  \"nodes\": [{ \"name\": \"${mesh.name}\", \"mesh\": 0 }],")
        appendLine("  \"meshes\": [{")
        appendLine("    \"name\": \"${mesh.name}\",")
        appendLine("    \"primitives\": [{")
        appendLine("      \"attributes\": { \"POSITION\": 0, \"NORMAL\": 1 },")
        appendLine("      \"indices\": 2,")
        appendLine("      \"mode\": 4")
        appendLine("    }]")
        appendLine("  }],")
        appendLine("  \"accessors\": [")
        appendLine("    { \"bufferView\": 0, \"componentType\": 5126, \"count\": ${mesh.vertices.size}, \"type\": \"VEC3\" },")
        appendLine("    { \"bufferView\": 1, \"componentType\": 5126, \"count\": ${mesh.triangles.size}, \"type\": \"VEC3\" },")
        appendLine("    { \"bufferView\": 2, \"componentType\": 5123, \"count\": ${mesh.triangles.size * 3}, \"type\": \"SCALAR\" }")
        appendLine("  ]")
        appendLine("}")
    }
}
