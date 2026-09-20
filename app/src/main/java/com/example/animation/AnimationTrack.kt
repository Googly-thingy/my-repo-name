package com.example.animation

import com.example.model3d.ModelTransform

data class AnimationTrack(
    val name: String = "Turntable Sequence",
    val durationSec: Float = 4.0f,
    val keyframes: List<Keyframe> = MovementPreset.TURNTABLE_360.generateKeyframes(),
    val isLooping: Boolean = true
) {
    val sortedKeyframes: List<Keyframe> by lazy {
        keyframes.sortedBy { it.timeSec }
    }

    /**
     * Samples the model transform at an arbitrary timestamp t (in seconds).
     */
    fun sampleTransform(currentTime: Float): ModelTransform {
        if (keyframes.isEmpty()) return ModelTransform()

        val activeTime = if (isLooping && durationSec > 0f) {
            val mod = currentTime % durationSec
            if (mod < 0f) mod + durationSec else mod
        } else {
            currentTime.coerceIn(0f, durationSec)
        }

        val sorted = sortedKeyframes
        if (activeTime <= sorted.first().timeSec) {
            return sorted.first().toModelTransform()
        }
        if (activeTime >= sorted.last().timeSec) {
            return sorted.last().toModelTransform()
        }

        // Find surrounding keyframes
        var prev = sorted.first()
        var next = sorted.last()
        for (i in 0 until sorted.size - 1) {
            if (activeTime >= sorted[i].timeSec && activeTime <= sorted[i + 1].timeSec) {
                prev = sorted[i]
                next = sorted[i + 1]
                break
            }
        }

        val timeDiff = next.timeSec - prev.timeSec
        val rawT = if (timeDiff > 0.0001f) (activeTime - prev.timeSec) / timeDiff else 0f
        val easedT = Keyframe.applyEasing(rawT, prev.easing)

        // Interpolate position
        val posX = prev.posX + (next.posX - prev.posX) * easedT
        val posY = prev.posY + (next.posY - prev.posY) * easedT
        val posZ = prev.posZ + (next.posZ - prev.posZ) * easedT

        // Interpolate rotation
        val rotX = prev.rotX + (next.rotX - prev.rotX) * easedT
        val rotY = prev.rotY + (next.rotY - prev.rotY) * easedT
        val rotZ = prev.rotZ + (next.rotZ - prev.rotZ) * easedT

        // Interpolate scale
        val scale = prev.scale + (next.scale - prev.scale) * easedT

        return ModelTransform(
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
    }

    fun addOrUpdateKeyframe(keyframe: Keyframe): AnimationTrack {
        val updated = keyframes.filterNot { it.id == keyframe.id || kotlin.math.abs(it.timeSec - keyframe.timeSec) < 0.05f }.toMutableList()
        updated.add(keyframe)
        return copy(keyframes = updated.sortedBy { it.timeSec })
    }

    fun removeKeyframe(id: String): AnimationTrack {
        if (keyframes.size <= 1) return this // Keep at least one keyframe
        return copy(keyframes = keyframes.filterNot { it.id == id })
    }

    fun withPreset(preset: MovementPreset): AnimationTrack {
        return copy(
            name = preset.title,
            durationSec = preset.defaultDurationSec,
            keyframes = preset.generateKeyframes()
        )
    }
}
