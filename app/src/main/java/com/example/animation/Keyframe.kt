package com.example.animation

import com.example.model3d.ModelTransform
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

enum class EasingType(val label: String) {
    SMOOTH("Smooth (Ease In-Out)"),
    LINEAR("Linear (Constant)"),
    BOUNCE("Bounce"),
    ELASTIC("Elastic Spring")
}

data class Keyframe(
    val id: String = "kf_${System.currentTimeMillis()}_${(0..9999).random()}",
    val timeSec: Float,
    val posX: Float = 0f,
    val posY: Float = 0f,
    val posZ: Float = 0f,
    val rotX: Float = 0f,
    val rotY: Float = 0f,
    val rotZ: Float = 0f,
    val scale: Float = 1f,
    val easing: EasingType = EasingType.SMOOTH
) {
    fun toModelTransform(): ModelTransform =
        ModelTransform(
            posX = posX,
            posY = posY,
            posZ = posZ,
            rotXDeg = rotX,
            rotYDeg = rotY,
            rotZDeg = rotZ,
            scaleX = scale,
            scaleY = scale,
            scaleZ = scale
        )

    companion object {
        fun applyEasing(t: Float, type: EasingType): Float {
            val clamped = t.coerceIn(0f, 1f)
            return when (type) {
                EasingType.LINEAR -> clamped
                EasingType.SMOOTH -> {
                    // Smoothstep / cosine ease in-out
                    0.5f * (1f - cos(clamped * PI.toFloat()))
                }
                EasingType.BOUNCE -> {
                    var x = clamped
                    val n1 = 7.5625f
                    val d1 = 2.75f
                    if (x < 1f / d1) {
                        n1 * x * x
                    } else if (x < 2f / d1) {
                        x -= 1.5f / d1
                        n1 * x * x + 0.75f
                    } else if (x < 2.5f / d1) {
                        x -= 2.25f / d1
                        n1 * x * x + 0.9375f
                    } else {
                        x -= 2.625f / d1
                        n1 * x * x + 0.984375f
                    }
                }
                EasingType.ELASTIC -> {
                    if (clamped == 0f || clamped == 1f) clamped
                    else {
                        val p = 0.3f
                        val s = p / 4f
                        val x = clamped - 1f
                        -(2.0.pow(10.0 * x) * sin((x - s) * (2f * PI) / p)).toFloat()
                    }
                }
            }
        }
    }
}
