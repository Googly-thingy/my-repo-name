package com.example.animation

import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

data class FramerateSettings(
    val fps: Int = 60
) {
    val frameDurationMs: Float get() = 1000f / fps.coerceAtLeast(1)
    val frameDurationSec: Float get() = 1f / fps.coerceAtLeast(1)

    fun totalFrames(durationSec: Float): Int =
        ceil(durationSec * fps).toInt()

    fun frameIndexToTime(frameIdx: Int): Float =
        frameIdx * frameDurationSec

    fun timeToFrameIndex(timeSec: Float): Int =
        (timeSec * fps).roundToInt()

    fun nextFrameTime(currentTimeSec: Float, maxDurationSec: Float): Float {
        val currentFrame = timeToFrameIndex(currentTimeSec)
        val nextFrame = currentFrame + 1
        return (nextFrame * frameDurationSec).coerceAtMost(maxDurationSec)
    }

    fun prevFrameTime(currentTimeSec: Float): Float {
        val currentFrame = timeToFrameIndex(currentTimeSec)
        val prevFrame = (currentFrame - 1).coerceAtLeast(0)
        return prevFrame * frameDurationSec
    }

    val displayInfo: String
        get() = String.format(Locale.US, "%d FPS (%.1f ms/frame)", fps, frameDurationMs)

    companion object {
        val PRESET_12_FPS = FramerateSettings(12) // Stop-motion / Claymation
        val PRESET_24_FPS = FramerateSettings(24) // Film Cinema Standard
        val PRESET_30_FPS = FramerateSettings(30) // Broadcast / Mobile standard
        val PRESET_60_FPS = FramerateSettings(60) // Ultra-smooth 60fps
        val PRESET_120_FPS = FramerateSettings(120) // High-Speed Display

        val COMMON_PRESETS = listOf(
            12 to "12 FPS • Stop Motion",
            24 to "24 FPS • Cinema 24p",
            30 to "30 FPS • Mobile Video",
            60 to "60 FPS • Smooth 60p",
            120 to "120 FPS • High Speed"
        )
    }
}
