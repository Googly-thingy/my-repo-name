package com.example.model3d

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Vector3(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
) {
    operator fun plus(other: Vector3): Vector3 =
        Vector3(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: Vector3): Vector3 =
        Vector3(x - other.x, y - other.y, z - other.z)

    operator fun times(scalar: Float): Vector3 =
        Vector3(x * scalar, y * scalar, z * scalar)

    operator fun div(scalar: Float): Vector3 =
        if (scalar != 0f) Vector3(x / scalar, y / scalar, z / scalar) else this

    fun dot(other: Vector3): Float =
        x * other.x + y * other.y + z * other.z

    fun cross(other: Vector3): Vector3 =
        Vector3(
            x = y * other.z - z * other.y,
            y = z * other.x - x * other.z,
            z = x * other.y - y * other.x
        )

    fun length(): Float =
        sqrt(x * x + y * y + z * z)

    fun lengthSquared(): Float =
        x * x + y * y + z * z

    fun normalized(): Vector3 {
        val len = length()
        return if (len > 0.00001f) this / len else Vector3(0f, 1f, 0f)
    }

    fun rotateX(rad: Float): Vector3 {
        val c = cos(rad)
        val s = sin(rad)
        return Vector3(x, y * c - z * s, y * s + z * c)
    }

    fun rotateY(rad: Float): Vector3 {
        val c = cos(rad)
        val s = sin(rad)
        return Vector3(x * c + z * s, y, -x * s + z * c)
    }

    fun rotateZ(rad: Float): Vector3 {
        val c = cos(rad)
        val s = sin(rad)
        return Vector3(x * c - y * s, x * s + y * c, z)
    }

    fun rotateEuler(pitchDeg: Float, yawDeg: Float, rollDeg: Float): Vector3 {
        val pRad = Math.toRadians(pitchDeg.toDouble()).toFloat()
        val yRad = Math.toRadians(yawDeg.toDouble()).toFloat()
        val rRad = Math.toRadians(rollDeg.toDouble()).toFloat()
        return this.rotateY(yRad).rotateX(pRad).rotateZ(rRad)
    }

    companion object {
        val ZERO = Vector3(0f, 0f, 0f)
        val UP = Vector3(0f, 1f, 0f)
        val FORWARD = Vector3(0f, 0f, 1f)
        val RIGHT = Vector3(1f, 0f, 0f)

        fun lerp(start: Vector3, end: Vector3, t: Float): Vector3 {
            val clamped = t.coerceIn(0f, 1f)
            return Vector3(
                x = start.x + (end.x - start.x) * clamped,
                y = start.y + (end.y - start.y) * clamped,
                z = start.z + (end.z - start.z) * clamped
            )
        }
    }
}
